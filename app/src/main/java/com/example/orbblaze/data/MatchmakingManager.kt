package com.example.orbblaze.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MatchmakingManager(private val authManager: AuthManager) {

    private val db = FirebaseFirestore.getInstance()
    private val roomsCollection = db.collection("gameRooms")

    fun findOrCreateRoom(targetRoomId: String? = null): Flow<GameRoom?> = callbackFlow {
        val currentUser = authManager.currentUser
        var roomListener: ListenerRegistration? = null

        if (currentUser == null) {
            trySend(null)
            close()
        } else {
            val myPlayerState = GameRoom(
                userId = currentUser.uid,
                displayName = currentUser.displayName ?: "Jugador",
                avatarUrl = currentUser.photoUrl?.toString()
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
                                transaction.update(roomRef, "players.${currentUser.uid}", myPlayerState)
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
                    val openRoomsQuery = roomsCollection
                        .whereEqualTo("status", "WAITING")
                        .whereEqualTo("playerCount", 1)
                        .limit(1)

                    val querySnapshot = openRoomsQuery.get().await()

                    if (querySnapshot.documents.isNotEmpty()) {
                        val doc = querySnapshot.documents.first()
                        val targetId = doc.id

                        val joined = db.runTransaction { transaction ->
                            val roomRef = roomsCollection.document(targetId)
                            val roomSnap = transaction.get(roomRef)
                            if (roomSnap.exists()) {
                                val players = roomSnap.get("players") as? Map<*, *>
                                if (players?.containsKey(currentUser.uid) == true) return@runTransaction false

                                val currentCount = roomSnap.getLong("playerCount") ?: 0
                                if (currentCount == 1L) {
                                    transaction.update(roomRef, "players.${currentUser.uid}", myPlayerState)
                                    transaction.update(roomRef, "playerCount", 2)
                                    transaction.update(roomRef, "status", "PLAYING")
                                    transaction.update(roomRef, "updatedAt", FieldValue.serverTimestamp())
                                    true
                                } else false
                            } else false
                        }.await()
                        if (joined) roomId = targetId
                    }
                }

                if (roomId == null) {
                    val newRoomId = targetRoomId ?: UUID.randomUUID().toString()
                    val newRoom = GameRoom(
                        roomId = newRoomId,
                        players = mapOf(currentUser.uid to myPlayerState),
                        playerCount = 1,
                        status = "WAITING"
                    )
                    roomsCollection.document(newRoomId).set(newRoom).await()
                    roomId = newRoomId
                }

                val finalRoomId = roomId
                if (finalRoomId != null) {
                    roomListener = roomsCollection.document(finalRoomId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(null)
                                return@addSnapshotListener
                            }
                            if (snapshot != null && snapshot.exists()) {
                                val updatedRoom = snapshot.toObject(GameRoom::class.java)
                                trySend(updatedRoom)
                            } else {
                                trySend(null)
                                close()
                            }
                        }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                trySend(null)
            }
        }

        awaitClose { roomListener?.remove() }
    }

    suspend fun addBotToRoom(roomId: String) {
        val botId = "BOT_${UUID.randomUUID().toString().take(8)}"
        val botNames = listOf("OrbMaster", "PandaBot", "BlazeRunner", "BubbleGhost", "NeoPlayer", "ZenShot")
        val botAvatars = listOf(
            "https://api.dicebear.com/7.x/bottts/png?seed=B1",
            "https://api.dicebear.com/7.x/bottts/png?seed=B2",
            "https://api.dicebear.com/7.x/bottts/png?seed=B3"
        )

        val botState = GameRoom(
            userId = botId,
            displayName = botNames.random(),
            avatarUrl = botAvatars.random(),
            bot = true
        )

        try {
            db.runTransaction { transaction ->
                val roomRef = roomsCollection.document(roomId)
                val roomSnap = transaction.get(roomRef)
                if (roomSnap.exists() && roomSnap.getLong("playerCount") == 1L) {
                    transaction.update(roomRef, "players.$botId", botState)
                    transaction.update(roomRef, "playerCount", 2)
                    transaction.update(roomRef, "status", "PLAYING")
                    transaction.update(roomRef, "updatedAt", FieldValue.serverTimestamp())
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun leaveRoom(roomId: String) {
        val myId = authManager.currentUser?.uid ?: return
        try {
            db.runTransaction { transaction ->
                val roomRef = roomsCollection.document(roomId)
                val roomSnap = transaction.get(roomRef)
                if (!roomSnap.exists()) return@runTransaction

                val currentStatus = roomSnap.getString("status") ?: "WAITING"
                val currentCount = roomSnap.getLong("playerCount") ?: 0
                
                if (currentCount <= 1L) {
                    transaction.delete(roomRef)
                } else {
                    if (currentStatus == "PLAYING") {
                        val players = roomSnap.get("players") as? Map<*, *>
                        val remainingPlayerId = players?.keys?.firstOrNull { it != myId } as? String
                        
                        transaction.update(roomRef, "status", "FINISHED")
                        transaction.update(roomRef, "winnerId", remainingPlayerId)
                    }
                    
                    transaction.update(roomRef, "players.$myId", FieldValue.delete())
                    transaction.update(roomRef, "playerCount", currentCount - 1)

                    @Suppress("UNCHECKED_CAST")
                    val playersSnap = roomSnap.get("players") as? Map<String, Any>
                    val remainingPlayers = playersSnap?.keys?.filter { it != myId }
                    
                    if (remainingPlayers?.size == 1 && remainingPlayers.first().startsWith("BOT_")) {
                        transaction.delete(roomRef)
                    }
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun endGame(roomId: String, winnerId: String) {
        try {
            roomsCollection.document(roomId).update(
                mapOf(
                    "status" to "FINISHED",
                    "winnerId" to winnerId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updatePlayerState(roomId: String, score: Int, attack: String?) {
        val myId = authManager.currentUser?.uid ?: return
        val updates = mutableMapOf(
            "players.$myId.score" to score,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        attack?.let { updates["players.$myId.lastAttack"] = it }
        try {
            roomsCollection.document(roomId).update(updates).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updatePlayerRematchStatus(roomId: String, ready: Boolean) {
        val myId = authManager.currentUser?.uid ?: return
        try {
            db.runTransaction { transaction ->
                val roomRef = roomsCollection.document(roomId)
                val roomSnap = transaction.get(roomRef)
                if (!roomSnap.exists()) return@runTransaction
                
                val roomObj = roomSnap.toObject(GameRoom::class.java) ?: return@runTransaction

                transaction.update(roomRef, "players.$myId.rematchReady", ready)

                val otherPlayer = roomObj.players.values.firstOrNull { it.userId != myId }
                // Reforzamos detección de bot por ID además del campo boolean
                val isOtherBot = otherPlayer?.bot == true || otherPlayer?.userId?.startsWith("BOT_") == true

                if (ready && (otherPlayer?.rematchReady == true || isOtherBot)) {
                    transaction.update(roomRef, "status", "PLAYING")
                    transaction.update(roomRef, "winnerId", FieldValue.delete())
                    transaction.update(roomRef, "updatedAt", FieldValue.serverTimestamp())

                    roomObj.players.keys.forEach { uid ->
                        transaction.update(roomRef, "players.$uid.score", 0)
                        transaction.update(roomRef, "players.$uid.rematchReady", false)
                        transaction.update(roomRef, "players.$uid.lastAttack", FieldValue.delete())
                        transaction.update(roomRef, "players.$uid.currentReaction", FieldValue.delete())
                        transaction.update(roomRef, "players.$uid.dangerLevel", 0f)
                        transaction.update(roomRef, "players.$uid.bubblesPopped", 0)
                        transaction.update(roomRef, "players.$uid.maxCombo", 0)
                        transaction.update(roomRef, "players.$uid.attacksSent", 0)
                    }
                } else if (ready) {
                    transaction.update(roomRef, "status", "REMATCH_REQUESTED")
                    transaction.update(roomRef, "updatedAt", FieldValue.serverTimestamp())
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendReaction(roomId: String, emoji: String) {
        val myId = authManager.currentUser?.uid ?: return
        try {
            roomsCollection.document(roomId).update(
                mapOf(
                    "players.$myId.currentReaction" to emoji,
                    "players.$myId.reactionTimestamp" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
