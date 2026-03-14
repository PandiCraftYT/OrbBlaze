package com.example.orbblaze.data

import android.util.Log
import com.example.orbblaze.domain.model.DuelMatch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchHistoryManager @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun saveMatch(userId: String, match: DuelMatch) {
        try {
            usersCollection.document(userId)
                .collection("matchHistory")
                .add(match)
                .await()
        } catch (e: Exception) {
            Log.e("MatchHistoryManager", "Error al guardar partida: ${e.message}")
        }
    }

    suspend fun getMatchHistory(userId: String): List<DuelMatch> {
        return try {
            val snapshot = usersCollection.document(userId)
                .collection("matchHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
            
            snapshot.toObjects(DuelMatch::class.java)
        } catch (e: Exception) {
            Log.e("MatchHistoryManager", "Error al obtener historial: ${e.message}")
            emptyList()
        }
    }
}
