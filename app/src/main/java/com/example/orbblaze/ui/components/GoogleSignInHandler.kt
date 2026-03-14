package com.example.orbblaze.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.orbblaze.data.AuthManager
import com.example.orbblaze.data.SettingsManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun rememberGoogleSignInHandler(
    authManager: AuthManager,
    settingsManager: SettingsManager,
    onSuccess: () -> Unit = {}
): () -> Unit {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Usamos el ID de cliente WEB (Tipo 3) de tu google-services.json
    val webClientId = "16414219373-43f70abac7dp5v3tbvvq6lndspdcsh0i.apps.googleusercontent.com"

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    scope.launch {
                        val currentLocalProgress = settingsManager.getSyncableData()
                        val linkResult = authManager.linkWithGoogle(idToken)
                        
                        if (linkResult.isSuccess) {
                            authManager.saveProgressToCloud(currentLocalProgress)
                            val cloudData = authManager.loadProgressFromCloud()
                            cloudData?.let { settingsManager.updateFromSyncableData(it) }
                            Toast.makeText(context, "Sesión sincronizada", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        } else {
                            val errorMsg = linkResult.exceptionOrNull()?.message?.lowercase() ?: ""
                            if (errorMsg.contains("associated") || errorMsg.contains("already-in-use") || errorMsg.contains("collision")) {
                                authManager.deleteCurrentUser()
                                val user = authManager.signInWithGoogle(idToken)
                                if (user != null) {
                                    val cloudData = authManager.loadProgressFromCloud()
                                    if (cloudData != null && cloudData.isNotEmpty()) {
                                        settingsManager.updateFromSyncableData(cloudData)
                                    } else {
                                        authManager.saveProgressToCloud(currentLocalProgress)
                                    }
                                    Toast.makeText(context, "Sesión iniciada con éxito", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                }
                            } else {
                                Toast.makeText(context, "Error al vincular cuenta", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val statusCode = (e as? ApiException)?.statusCode
                val message = when(statusCode) {
                    7 -> "Error 7: Red (Revisa conexión)"
                    8 -> "Error 8: Error interno"
                    10 -> "Error 10: SHA-1 o Paquete NO coinciden en Firebase"
                    12500 -> "Error 12500: Revisa Correo de Soporte en Firebase"
                    else -> "Error login: $statusCode"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    return {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        launcher.launch(client.signInIntent)
    }
}
