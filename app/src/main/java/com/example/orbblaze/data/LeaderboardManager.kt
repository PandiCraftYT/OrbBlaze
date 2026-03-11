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
    private val COLLECTION_NAME = "leaderboards"

    /**
     * Obtiene el ranking de un modo específico.
     * Ahora busca en campos como 'score_CLASSIC', 'score_DUEL', etc.
     */
    fun getLeaderboard(mode: String): Flow<List<LeaderboardEntry>> = callbackFlow {
        val scoreField = "score_$mode" // Campo dinámico según el modo
        Log.d("LeaderboardManager", "Obteniendo ranking unificado. Ordenando por: $scoreField")

        val listener = firestore.collection(COLLECTION_NAME)
            .orderBy(scoreField, Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LeaderboardManager", "Error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val scoreValue = doc.getLong(scoreField)?.toInt() ?: 0
                        // Solo incluimos si el puntaje es mayor a 0 (para no mostrar gente que no ha jugado ese modo)
                        if (scoreValue > 0 || mode == "DUEL") {
                            LeaderboardEntry(
                                userId = doc.id,
                                username = doc.getString("username") ?: "Jugador",
                                score = scoreValue,
                                avatarUrl = doc.getString("avatarUrl")
                            )
                        } else null
                    } catch (e: Exception) { null }
                } ?: emptyList()

                trySend(entries)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateScore(userId: String, username: String, score: Int, avatarUrl: String?, mode: String) {
        val scoreField = "score_$mode"
        val entry = mutableMapOf(
            "userId" to userId,
            "username" to username,
            "avatarUrl" to avatarUrl,
            "timestamp" to FieldValue.serverTimestamp(),
            scoreField to score // Actualiza solo el campo del modo correspondiente
        )

        try {
            firestore.collection(COLLECTION_NAME).document(userId)
                .set(entry, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("LeaderboardManager", "Error al actualizar $mode: ${e.message}")
        }
    }

    suspend fun updateDuelRating(userId: String, username: String, avatarUrl: String?, isWin: Boolean) {
        val scoreField = "score_DUEL"
        val docRef = firestore.collection(COLLECTION_NAME).document(userId)

        try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentScore = if (snapshot.exists()) {
                    snapshot.getLong(scoreField)?.toInt() ?: 1000
                } else {
                    1000
                }

                val pointsChange = if (isWin) 25 else -20
                val newScore = (currentScore + pointsChange).coerceAtLeast(0)

                val entry = mapOf(
                    "userId" to userId,
                    "username" to username,
                    "avatarUrl" to avatarUrl,
                    "timestamp" to FieldValue.serverTimestamp(),
                    scoreField to newScore
                )
                transaction.set(docRef, entry, SetOptions.merge())
            }.await()
        } catch (e: Exception) {
            Log.e("LeaderboardManager", "Error en Rating Duelo: ${e.message}")
        }
    }

    suspend fun syncDuelRating(userId: String, username: String, avatarUrl: String?, currentElo: Int) {
        val scoreField = "score_DUEL"
        try {
            val entry = mapOf(
                "userId" to userId,
                "username" to username,
                "avatarUrl" to avatarUrl,
                "timestamp" to FieldValue.serverTimestamp(),
                scoreField to currentElo
            )
            firestore.collection(COLLECTION_NAME).document(userId)
                .set(entry, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("LeaderboardManager", "Error sync Duelo: ${e.message}")
        }
    }
}