package com.example.orbblaze.data

import android.content.Context
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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

    // --- SISTEMA DE AMIGOS ---

    suspend fun findUserByPlayerId(playerId: String): Map<String, Any>? {
        val cleanId = playerId.trim().uppercase()
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("playerId", cleanId)
                .get().await()
            val doc = snapshot.documents.firstOrNull()
            doc?.data?.plus("uid" to doc.id)
        } catch (e: Exception) {
            null
        }
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
            db.collection("users").document(toUid).collection("friend_requests").document(fromUid).set(request).await()
            true
        } catch (e: Exception) { false }
    }

    fun getFriendRequests(): Flow<List<Map<String, Any>>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val listener = db.collection("users").document(uid).collection("friend_requests")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val requests = snapshot.documents.mapNotNull { it.data?.plus("fromUid" to it.id) }
                    trySend(requests)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun acceptFriendRequest(friendUid: String): Boolean {
        val myUid = auth.currentUser?.uid ?: return false
        return try {
            db.runBatch { batch ->
                batch.set(db.collection("users").document(myUid), mapOf("friends" to FieldValue.arrayUnion(friendUid)), SetOptions.merge())
                batch.set(db.collection("users").document(friendUid), mapOf("friends" to FieldValue.arrayUnion(myUid)), SetOptions.merge())
                batch.delete(db.collection("users").document(myUid).collection("friend_requests").document(friendUid))
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

    /**
     * Elimina a un amigo de la lista.
     */
    suspend fun removeFriend(friendUid: String): Boolean {
        val myUid = auth.currentUser?.uid ?: return false
        return try {
            db.runBatch { batch ->
                batch.update(db.collection("users").document(myUid), "friends", FieldValue.arrayRemove(friendUid))
                batch.update(db.collection("users").document(myUid), "favoriteFriends", FieldValue.arrayRemove(friendUid))
                batch.update(db.collection("users").document(friendUid), "friends", FieldValue.arrayRemove(myUid))
                batch.update(db.collection("users").document(friendUid), "favoriteFriends", FieldValue.arrayRemove(myUid))
            }.await()
            true
        } catch (e: Exception) { false }
    }

    /**
     * Alterna un amigo como favorito.
     */
    suspend fun toggleFavoriteFriend(friendUid: String, isFavorite: Boolean): Boolean {
        val myUid = auth.currentUser?.uid ?: return false
        return try {
            val update = if (isFavorite) FieldValue.arrayUnion(friendUid) else FieldValue.arrayRemove(friendUid)
            db.collection("users").document(myUid).update("favoriteFriends", update).await()
            true
        } catch (e: Exception) { false }
    }

    fun getFriends(): Flow<List<Map<String, Any>>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val friendUids = snapshot.get("friends") as? List<String> ?: emptyList()
                    val favoriteUids = snapshot.get("favoriteFriends") as? List<String> ?: emptyList()
                    
                    if (friendUids.isEmpty()) {
                        trySend(emptyList())
                    } else {
                        db.collection("users").whereIn(com.google.firebase.firestore.FieldPath.documentId(), friendUids)
                            .get().addOnSuccessListener { friendsSnapshot ->
                                val friendsData = friendsSnapshot.documents.mapNotNull { doc ->
                                    doc.data?.plus("uid" to doc.id)?.plus("isFavorite" to favoriteUids.contains(doc.id))
                                }
                                trySend(friendsData)
                            }
                    }
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    // --- PERSISTENCIA ---

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

    suspend fun deleteCloudProgress(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            db.collection("users").document(user.uid).delete().await()
            true
        } catch (e: Exception) { false }
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
