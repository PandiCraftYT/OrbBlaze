package com.example.orbblaze.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MatchmakingManager(private val authManager: AuthManager) {

    private val db = FirebaseFirestore.getInstance()
    private val roomsCollection = db.collection("gameRooms")

    fun findOrCreateRoom(targetRoomId: String? = null, userElo: Int = 1000): Flow<GameRoom?> = callbackFlow {
        val currentUser = authManager.currentUser
        var roomListener: ListenerRegistration? = null

        if (currentUser == null) {
            trySend(null)
            close()
        } else {
            val myPlayerMap = mapOf(
                "userId" to currentUser.uid,
                "displayName" to (currentUser.displayName ?: "Jugador"),
                "avatarUrl" to currentUser.photoUrl?.toString(),
                "elo" to userElo,
                "score" to 0,
                "dangerLevel" to 0f,
                "ready" to true,
                "lastHeartbeat" to System.currentTimeMillis(),
                "reactionTimestamp" to System.currentTimeMillis()
            )

            try {
                var roomId: String? = null

                if (targetRoomId != null) {
                    val joined = db.runTransaction { transaction ->
                        val roomRef = roomsCollection.document(targetRoomId)
                        val roomSnap = transaction.get(roomRef)
                        if (roomSnap.exists()) {
                            val players = roomSnap.get("players") as? Map<*, *>
                            if (players?.containsKey(currentUser.uid) == true) return@runTransaction true

                            val currentCount = roomSnap.getLong("playerCount") ?: 0
                            if (currentCount == 1L) {
                                transaction.update(roomRef, "players.${currentUser.uid}", myPlayerMap)
                                transaction.update(roomRef, "playerCount", 2)
                                transaction.update(roomRef, "status", "PLAYING")
                                transaction.update(roomRef, "updatedAt", FieldValue.serverTimestamp())
                                true
                            } else false
                        } else false
                    }.await()
                    if (joined) roomId = targetRoomId
                }

                if (roomId == null && targetRoomId == null) {
                    val querySnapshot = try {
                        roomsCollection
                            .whereEqualTo("status", "WAITING")
                            .whereEqualTo("playerCount", 1)
                            .whereLessThanOrEqualTo("minElo", userElo)
                            .limit(5)
                            .get().await()
                    } catch (e: Exception) {
                        roomsCollection
                            .whereEqualTo("status", "WAITING")
                            .whereEqualTo("playerCount", 1)
                            .limit(5)
                            .get().await()
                    }

                    val suitableDoc = querySnapshot.documents.firstOrNull { doc ->
                        val maxElo = doc.getLong("maxElo") ?: 9999
                        userElo <= maxElo
                    } ?: querySnapshot.documents.firstOrNull()

                    if (suitableDoc != null) {
                        val joined = db.runTransaction { transaction ->
                            val roomRef = roomsCollection.document(suitableDoc.id)
                            val roomSnap = transaction.get(roomRef)
                            val currentCount = roomSnap.getLong("playerCount") ?: 0
                            if (currentCount == 1L) {
                                transaction.update(roomRef, "players.${currentUser.uid}", myPlayerMap)
                                transaction.update(roomRef, "playerCount", 2)
                                transaction.update(roomRef, "status", "PLAYING")
                                transaction.update(roomRef, "updatedAt", FieldValue.serverTimestamp())
                                true
                            } else false
                        }.await()
                        if (joined) roomId = suitableDoc.id
                    }
                }

                if (roomId == null) {
                    val newId = targetRoomId ?: UUID.randomUUID().toString()
                    val eloRange = 250
                    val newRoom = mapOf(
                        "roomId" to newId,
                        "players" to mapOf(currentUser.uid to myPlayerMap),
                        "playerCount" to 1,
                        "status" to "WAITING",
                        "minElo" to (userElo - eloRange).coerceAtLeast(0),
                        "maxElo" to userElo + eloRange,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    roomsCollection.document(newId).set(newRoom).await()
                    roomId = newId
                }

                roomListener = roomsCollection.document(roomId!!)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null && snapshot.exists()) {
                            trySend(snapshot.toObject(GameRoom::class.java))
                        }
                    }

            } catch (e: Exception) {
                trySend(null)
            }
        }
        awaitClose { roomListener?.remove() }
    }

    suspend fun addBotToRoom(roomId: String) {
        val botId = "BOT_${UUID.randomUUID().toString().take(6)}"
        val botMap = mapOf(
            "userId" to botId,
            "displayName" to listOf("OrbMaster", "BlazeBot", "NeoPanda").random(),
            "avatarUrl" to "https://api.dicebear.com/7.x/bottts/png?seed=$botId",
            "bot" to true,
            "elo" to 1000,
            "score" to 0,
            "dangerLevel" to 0f,
            "lastHeartbeat" to System.currentTimeMillis(),
            "reactionTimestamp" to System.currentTimeMillis()
        )

        try {
            db.runTransaction { transaction ->
                val roomRef = roomsCollection.document(roomId)
                val roomSnap = transaction.get(roomRef)
                if (roomSnap.exists() && (roomSnap.getLong("playerCount") ?: 0) == 1L) {
                    transaction.update(roomRef, "players.$botId", botMap)
                    transaction.update(roomRef, "playerCount", 2)
                    transaction.update(roomRef, "status", "PLAYING")
                    transaction.update(roomRef, "updatedAt", FieldValue.serverTimestamp())
                }
            }.await()
        } catch (e: Exception) {
            Log.e("Matchmaking", "Error BOT: ${e.message}")
        }
    }

    suspend fun updateHeartbeat(roomId: String) {
        val myId = authManager.currentUser?.uid ?: return
        try {
            roomsCollection.document(roomId).update("players.$myId.lastHeartbeat", System.currentTimeMillis()).await()
        } catch (e: Exception) { }
    }

    suspend fun leaveRoom(roomId: String) {
        val myId = authManager.currentUser?.uid ?: return
        try {
            db.runTransaction { transaction ->
                val roomRef = roomsCollection.document(roomId)
                val roomSnap = transaction.get(roomRef)
                if (!roomSnap.exists()) return@runTransaction

                val players = roomSnap.get("players") as? Map<*, *> ?: emptyMap<String, Any>()
                val remainingPlayers = players.filterKeys { it != myId }
                
                if (remainingPlayers.isEmpty() || remainingPlayers.values.any { 
                    (it as? Map<*, *>)?.get("bot") == true || (it as? Map<*, *>)?.get("userId")?.toString()?.startsWith("BOT_") == true 
                }) {
                    transaction.delete(roomRef)
                } else {
                    transaction.update(roomRef, "players.$myId", FieldValue.delete())
                    transaction.update(roomRef, "playerCount", remainingPlayers.size)
                    transaction.update(roomRef, "status", "FINISHED")
                }
            }.await()
        } catch (e: Exception) {
            Log.e("Matchmaking", "Error al salir: ${e.message}")
        }
    }

    suspend fun endGame(roomId: String, winnerId: String) {
        try {
            db.runTransaction { transaction ->
                val roomRef = roomsCollection.document(roomId)
                val snap = transaction.get(roomRef)
                if (snap.exists() && snap.getString("status") != "FINISHED") {
                    transaction.update(roomRef, mapOf(
                        "status" to "FINISHED",
                        "winnerId" to winnerId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                }
            }.await()
        } catch (e: Exception) { }
    }

    suspend fun updatePlayerRematchStatus(roomId: String, ready: Boolean) {
        val myId = authManager.currentUser?.uid ?: return
        try {
            db.runTransaction { transaction ->
                val roomRef = roomsCollection.document(roomId)
                val roomObj = transaction.get(roomRef).toObject(GameRoom::class.java) ?: return@runTransaction
                
                transaction.update(roomRef, "players.$myId.rematchReady", ready)
                
                val other = roomObj.players.values.firstOrNull { it.userId != myId }
                if (ready && (other?.rematchReady == true || other?.bot == true)) {
                    transaction.update(roomRef, "status", "PLAYING")
                    transaction.update(roomRef, "winnerId", FieldValue.delete())
                    roomObj.players.keys.forEach { uid ->
                        transaction.update(roomRef, "players.$uid.score", 0)
                        transaction.update(roomRef, "players.$uid.rematchReady", false)
                        transaction.update(roomRef, "players.$uid.dangerLevel", 0f)
                    }
                }
            }.await()
        } catch (e: Exception) { }
    }

    suspend fun sendReaction(roomId: String, emoji: String) {
        val myId = authManager.currentUser?.uid ?: return
        roomsCollection.document(roomId).update(
            "players.$myId.currentReaction", emoji,
            "players.$myId.reactionTimestamp", System.currentTimeMillis()
        )
    }
}
