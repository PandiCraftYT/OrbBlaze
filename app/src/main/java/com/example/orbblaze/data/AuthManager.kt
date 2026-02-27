package com.example.orbblaze.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                Result.failure(Exception("No se pudo establecer una sesión activa"))
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
            e.printStackTrace()
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun refreshUser() {
        try {
            val user = auth.currentUser
            if (user != null) {
                user.reload().await()
                _user.value = auth.currentUser
            }
        } catch (e: Exception) {
            handleAuthError(e)
        }
    }

    private fun handleAuthError(e: Exception) {
        if (e.message?.contains("user-not-found") == true || e.message?.contains("no longer valid") == true) {
            _sessionError.value = "Tu cuenta de Google ya no es válida o ha sido eliminada."
            auth.signOut()
            _user.value = null
        }
    }

    suspend fun saveProgressToCloud(data: Map<String, Any>): Boolean {
        val user = auth.currentUser ?: return false
        if (user.isAnonymous) return false 
        return try {
            db.collection("users").document(user.uid).set(data).await()
            true
        } catch (e: Exception) {
            handleAuthError(e)
            false
        }
    }

    suspend fun loadProgressFromCloud(): Map<String, Any>? {
        val user = auth.currentUser ?: return null
        if (user.isAnonymous) return null
        return try {
            val document = db.collection("users").document(user.uid).get().await()
            if (document.exists()) document.data else null
        } catch (e: Exception) {
            handleAuthError(e)
            null
        }
    }

    /**
     * Elimina físicamente el documento de progreso de Firestore para el usuario actual.
     */
    suspend fun deleteCloudProgress(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            db.collection("users").document(user.uid).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
