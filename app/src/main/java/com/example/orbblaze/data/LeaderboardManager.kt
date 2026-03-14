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

    fun getLeaderboard(mode: String): Flow<List<LeaderboardEntry>> = callbackFlow {
        val scoreField = "score_$mode"
        val listener = firestore.collection(COLLECTION_NAME)
            .orderBy(scoreField, Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val scoreValue = doc.getLong(scoreField)?.toInt() ?: 0
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
            scoreField to score
        )

        try {
            firestore.collection(COLLECTION_NAME).document(userId)
                .set(entry, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("LeaderboardManager", "Error al actualizar $mode: ${e.message}")
        }
    }

    /**
     * ✅ MEJORADO: Ahora acepta el ELO final calculado dinámicamente
     */
    suspend fun updateDuelRating(userId: String, username: String, avatarUrl: String?, finalElo: Int) {
        val scoreField = "score_DUEL"
        try {
            val entry = mapOf(
                "userId" to userId,
                "username" to username,
                "avatarUrl" to avatarUrl,
                "timestamp" to FieldValue.serverTimestamp(),
                scoreField to finalElo
            )
            firestore.collection(COLLECTION_NAME).document(userId)
                .set(entry, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("LeaderboardManager", "Error en Rating Duelo: ${e.message}")
        }
    }
}
