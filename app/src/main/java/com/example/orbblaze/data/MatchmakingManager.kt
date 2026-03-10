package com.example.orbblaze.data

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

    private var roomListener: ListenerRegistration? = null

    fun findOrCreateRoom(targetRoomId: String? = null): Flow<GameRoom?> = callbackFlow {
        val currentUser = authManager.currentUser ?: run { 
            trySend(null)
            close()
            return@callbackFlow 
        }
        
        val myPlayerState = PlayerState(
            userId = currentUser.uid,
            displayName = currentUser.displayName ?: "Jugador",
            avatarUrl = currentUser.photoUrl?.toString()
        )

        try {
            var roomId: String? = null

            // 1. Unirse a sala específica (por Invitación)
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

            // 2. Búsqueda de sala abierta (Matchmaking normal)
            if (roomId == null && targetRoomId == null) {
                // Consulta limpia: solo por estado y cantidad de jugadores
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
                            // Evitar unirse a su propia sala si quedó abierta por error
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

            // 3. Crear sala nueva si no se encontró ninguna
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

            // 4. Escuchar cambios en la sala asignada
            val finalRoomId = roomId ?: return@callbackFlow
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

        } catch (e: Exception) {
            e.printStackTrace()
            trySend(null)
        }

        awaitClose { roomListener?.remove() }
    }

    suspend fun leaveRoom(roomId: String) {
        roomListener?.remove()
        val myId = authManager.currentUser?.uid ?: return
        try {
            db.runTransaction { transaction ->
                val roomRef = roomsCollection.document(roomId)
                val roomSnap = transaction.get(roomRef)
                if (!roomSnap.exists()) return@runTransaction

                val currentCount = roomSnap.getLong("playerCount") ?: 0
                if (currentCount <= 1L) {
                    transaction.delete(roomRef)
                } else {
                    transaction.update(roomRef, "players.$myId", FieldValue.delete())
                    transaction.update(roomRef, "playerCount", currentCount - 1)
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
        val updates = mutableMapOf<String, Any>(
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
                val room = roomSnap.toObject(GameRoom::class.java) ?: return@runTransaction

                transaction.update(roomRef, "players.$myId.rematchReady", ready)

                val otherPlayer = room.players.values.firstOrNull { it.userId != myId }
                if (ready && otherPlayer?.rematchReady == true) {
                    transaction.update(roomRef, "status", "PLAYING")
                    room.players.keys.forEach { uid ->
                        transaction.update(roomRef, "players.$uid.score", 0)
                        transaction.update(roomRef, "players.$uid.rematchReady", false)
                        transaction.update(roomRef, "players.$uid.lastAttack", null)
                    }
                } else if (ready) {
                    transaction.update(roomRef, "status", "REMATCH_REQUESTED")
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
