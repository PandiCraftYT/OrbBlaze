package com.example.orbblaze.data

import android.util.Log
import com.example.orbblaze.domain.model.LeaderboardEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardManager @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()

    fun getLeaderboard(mode: String): Flow<List<LeaderboardEntry>> = callbackFlow {
        val collection = when (mode) {
            "TIME_ATTACK" -> "leaderboard_time_attack"
            "DUEL" -> "leaderboard_duel"
            else -> "leaderboard_classic"
        }

        Log.d("LeaderboardManager", "Iniciando escucha para colección: $collection")

        val listener = firestore.collection(collection)
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LeaderboardManager", "Error en Firestore: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                val entries = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(LeaderboardEntry::class.java)?.copy(userId = doc.id)
                    } catch (e: Exception) {
                        Log.e("LeaderboardManager", "Error mapeando documento ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()
                
                Log.d("LeaderboardManager", "Datos recibidos: ${entries.size} usuarios")
                trySend(entries)
            }
        awaitClose { 
            Log.d("LeaderboardManager", "Cerrando escucha de $collection")
            listener.remove() 
        }
    }

    suspend fun updateScore(userId: String, username: String, score: Int, avatarUrl: String?, mode: String) {
        val collection = when (mode) {
            "TIME_ATTACK" -> "leaderboard_time_attack"
            "DUEL" -> "leaderboard_duel"
            else -> "leaderboard_classic"
        }

        val entry = mapOf(
            "username" to username,
            "score" to score,
            "avatarUrl" to avatarUrl,
            "timestamp" to FieldValue.serverTimestamp()
        )

        try {
            Log.d("LeaderboardManager", "Subiendo puntuación a $collection para usuario $userId")
            firestore.collection(collection).document(userId)
                .set(entry, SetOptions.merge())
                .await()
            Log.d("LeaderboardManager", "Puntuación subida con éxito")
        } catch (e: Exception) {
            Log.e("LeaderboardManager", "Error al subir puntuación: ${e.message}")
        }
    }

    suspend fun updateDuelRating(userId: String, username: String, avatarUrl: String?, isWin: Boolean) {
        val docRef = firestore.collection("leaderboard_duel").document(userId)
        
        try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentScore = if (snapshot.exists()) {
                    snapshot.getLong("score")?.toInt() ?: 1000
                } else {
                    1000
                }
                
                val pointsChange = if (isWin) 25 else -20
                val newScore = (currentScore + pointsChange).coerceAtLeast(0)
                
                val entry = mapOf(
                    "username" to username,
                    "score" to newScore,
                    "avatarUrl" to avatarUrl,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                
                transaction.set(docRef, entry, SetOptions.merge())
            }.await()
        } catch (e: Exception) {
            Log.e("LeaderboardManager", "Error al actualizar rating de duelo: ${e.message}")
        }
    }
}
