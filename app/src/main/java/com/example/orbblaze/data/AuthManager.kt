package com.example.orbblaze.data

import android.content.Context
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _user = MutableStateFlow(auth.currentUser)
    val user = _user.asStateFlow()

    private val _sessionError = MutableStateFlow<String?>(null)
    val sessionError = _sessionError.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _user.value = firebaseAuth.currentUser
        }
    }

    fun clearSessionError() { _sessionError.value = null }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserAnonymous: Boolean
        get() = auth.currentUser?.isAnonymous ?: true

    fun getPlayerId(): String {
        val uid = auth.currentUser?.uid ?: return "INVITADO"
        return "ORB-${uid.takeLast(6).uppercase()}"
    }

    suspend fun signInAnonymously(): FirebaseUser? {
        return try {
            if (auth.currentUser == null) {
                val result = auth.signInAnonymously().await()
                _user.value = result.user
                result.user
            } else {
                auth.currentUser
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun linkWithGoogle(idToken: String): Result<FirebaseUser?> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = auth.currentUser ?: signInAnonymously()
            if (user != null) {
                val result = user.linkWithCredential(credential).await()
                _user.value = result.user
                Result.success(result.user)
            } else {
                Result.failure(Exception("No hay sesión activa"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): FirebaseUser? {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            _user.value = result.user
            result.user
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteCurrentUser() {
        try {
            auth.currentUser?.delete()?.await()
            _user.value = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun signOut(context: Context?) {
        auth.signOut()
        _user.value = null
        if (context != null) {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInClient.signOut()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun refreshUser() {
        try {
            auth.currentUser?.reload()?.await()
            _user.value = auth.currentUser
        } catch (e: Exception) {
            if (e.message?.contains("user-not-found") == true) {
                _sessionError.value = "Sesión expirada"
                auth.signOut()
                _user.value = null
            }
        }
    }

    // --- SISTEMA DE AMIGOS SEGURO ---

    suspend fun findUserByPlayerId(playerId: String): Map<String, Any>? {
        val cleanId = playerId.trim().uppercase()
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("playerId", cleanId)
                .get().await()
            val doc = snapshot.documents.firstOrNull()
            doc?.data?.plus("uid" to doc.id)
        } catch (e: Exception) { null }
    }

    suspend fun getUserProfile(uid: String): Map<String, Any>? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.data?.plus("uid" to doc.id)
        } catch (e: Exception) { null }
    }

    suspend fun sendFriendRequest(toUid: String): Boolean {
        val fromUid = auth.currentUser?.uid ?: return false
        if (fromUid == toUid) return false
        return try {
            val request = mapOf(
                "fromUid" to fromUid,
                "fromName" to (auth.currentUser?.displayName ?: "Jugador"),
                "fromPhoto" to auth.currentUser?.photoUrl?.toString(),
                "status" to "pending",
                "timestamp" to FieldValue.serverTimestamp()
            )
            // Escribimos en la colección del destinatario
            db.collection("users").document(toUid).collection("friend_requests").document(fromUid).set(request).await()
            true
        } catch (e: Exception) { false }
    }

    fun getFriendRequests(): Flow<List<Map<String, Any>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        
        val listener = db.collection("users").document(uid).collection("friend_requests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                if (snapshot != null) {
                    val requests = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data?.plus("fromUid" to doc.id)
                        
                        // ✅ AUTO-PROCESAMIENTO DE CONFIRMACIONES
                        // Si recibimos una confirmación de que alguien aceptó nuestra solicitud previa
                        if (data?.get("status") == "accepted_confirmation") {
                            val friendId = doc.id
                            // 1. Nos añadimos a nosotros mismos
                            db.collection("users").document(uid).update("friends", FieldValue.arrayUnion(friendId))
                            // 2. Borramos la confirmación
                            db.collection("users").document(uid).collection("friend_requests").document(friendId).delete()
                            null // No mostrar en la lista de solicitudes
                        } else {
                            data
                        }
                    }
                    trySend(requests)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun acceptFriendRequest(friendUid: String): Boolean {
        val myUid = auth.currentUser?.uid ?: return false
        return try {
            db.runBatch { batch ->
                // 1. Añadir a mi lista de amigos
                batch.set(db.collection("users").document(myUid), mapOf("friends" to FieldValue.arrayUnion(friendUid)), SetOptions.merge())
                
                // 2. Borrar la solicitud entrante
                batch.delete(db.collection("users").document(myUid).collection("friend_requests").document(friendUid))
                
                // 3. Notificar al otro enviando una "confirmación" a su buzón
                val confirmation = mapOf(
                    "fromUid" to myUid,
                    "status" to "accepted_confirmation",
                    "timestamp" to FieldValue.serverTimestamp()
                )
                batch.set(db.collection("users").document(friendUid).collection("friend_requests").document(myUid), confirmation)
            }.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun rejectFriendRequest(friendUid: String): Boolean {
        val myUid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("users").document(myUid).collection("friend_requests").document(friendUid).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun removeFriend(friendUid: String): Boolean {
        val myUid = auth.currentUser?.uid ?: return false
        return try {
            db.runBatch { batch ->
                batch.update(db.collection("users").document(myUid), "friends", FieldValue.arrayRemove(friendUid))
                batch.update(db.collection("users").document(myUid), "favoriteFriends", FieldValue.arrayRemove(friendUid))
                // Nota: El amigo deberá borrarnos manualmente o esperar a una futura Cloud Function
            }.await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun toggleFavoriteFriend(friendUid: String, isFavorite: Boolean): Boolean {
        val myUid = auth.currentUser?.uid ?: return false
        return try {
            val update = if (isFavorite) FieldValue.arrayUnion(friendUid) else FieldValue.arrayRemove(friendUid)
            db.collection("users").document(myUid).update("favoriteFriends", update).await()
            true
        } catch (e: Exception) { false }
    }

    fun getFriends(): Flow<List<Map<String, Any>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                if (snapshot != null && snapshot.exists()) {
                    val friendUids = snapshot.get("friends") as? List<String> ?: emptyList()
                    val favoriteUids = snapshot.get("favoriteFriends") as? List<String> ?: emptyList()
                    
                    if (friendUids.isEmpty()) {
                        trySend(emptyList())
                    } else {
                        db.collection("users").whereIn(com.google.firebase.firestore.FieldPath.documentId(), friendUids.take(10))
                            .get().addOnSuccessListener { friendsSnapshot ->
                                val friendsData = friendsSnapshot.documents.mapNotNull { doc ->
                                    doc.data?.plus("uid" to doc.id)?.plus("isFavorite" to favoriteUids.contains(doc.id))
                                }
                                trySend(friendsData)
                            }.addOnFailureListener { trySend(emptyList()) }
                    }
                } else { trySend(emptyList()) }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveProgressToCloud(data: Map<String, Any>): Boolean {
        val user = auth.currentUser ?: return false
        val extendedData = data.toMutableMap()
        extendedData["displayName"] = user.displayName ?: "Jugador"
        extendedData["isAnonymous"] = user.isAnonymous
        extendedData["playerId"] = getPlayerId()
        user.photoUrl?.let { extendedData["photoUrl"] = it.toString() }
        return try {
            db.collection("users").document(user.uid).set(extendedData, SetOptions.merge()).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun loadProgressFromCloud(): Map<String, Any>? {
        val user = auth.currentUser ?: return null
        return try {
            val doc = db.collection("users").document(user.uid).get().await()
            doc.data
        } catch (e: Exception) { null }
    }

    suspend fun updateProfile(displayName: String?, photoUrl: String?): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val updates = userProfileChangeRequest {
                displayName?.let { this.displayName = it }
                photoUrl?.let { this.photoUri = Uri.parse(it) }
            }
            user.updateProfile(updates).await()
            val data = mutableMapOf<String, Any>()
            displayName?.let { data["displayName"] = it }
            photoUrl?.let { data["photoUrl"] = it }
            data["playerId"] = getPlayerId()
            db.collection("users").document(user.uid).set(data, SetOptions.merge()).await()
            _user.value = auth.currentUser
            true
        } catch (e: Exception) { false }
    }
}
