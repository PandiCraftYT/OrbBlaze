package com.example.orbblaze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.orbblaze.ui.game.LevelScreen // Importamos tu pantalla de juego
import com.example.orbblaze.ui.theme.OrbBlazeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OrbBlazeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    LevelScreen()
                }
            }
        }
    }
}