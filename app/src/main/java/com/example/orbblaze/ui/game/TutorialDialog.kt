package com.example.orbblaze.ui.game

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TutorialTarget { NONE, SHOP, CANNON, NEXT_BUBBLE, SCORE }

data class TutorialStep(
    val title: String,
    val description: String,
    val icon: String,
    val target: TutorialTarget
)

@Composable
fun TutorialDialog(
    shopRect: Rect?,
    cannonRect: Rect?,
    nextBubbleRect: Rect?,
    scoreRect: Rect?,
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    val steps = listOf(
        TutorialStep("¡BIENVENIDO!", "Apunta y dispara para explotar 3 o más burbujas del mismo color.", "🎯", TutorialTarget.NONE),
        TutorialStep("MARCADOR", "Aquí puedes ver tu puntuación y monedas actuales.", "💰", TutorialTarget.SCORE),
        TutorialStep("TIENDA RÁPIDA", "Toca aquí para comprar objetos especiales como la Bola de Fuego.", "🏪", TutorialTarget.SHOP),
        TutorialStep("CAÑÓN PANDA", "Desliza para apuntar. ¡Toca al panda para cambiar el color!", "🐼", TutorialTarget.CANNON),
        TutorialStep("PRÓXIMA GEMA", "Este es el color de la siguiente gema que cargarás.", "🔮", TutorialTarget.NEXT_BUBBLE),
        TutorialStep("¡TODO LISTO!", "¡Disfruta de OrbBlaze y alcanza el récord más alto!", "✨", TutorialTarget.NONE)
    )

    val currentTarget = steps[currentStep].target
    val targetRect = when(currentTarget) {
        TutorialTarget.SHOP -> shopRect
        TutorialTarget.CANNON -> cannonRect
        TutorialTarget.NEXT_BUBBLE -> nextBubbleRect
        TutorialTarget.SCORE -> scoreRect
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Sin efecto de onda para que sea más limpio
            ) {
                if (currentStep < steps.size - 1) currentStep++ else onComplete()
            }
    ) {
        // --- CAPA DE FOCO (SPOTLIGHT) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spotlightPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                targetRect?.let {
                    val radius = (it.width.coerceAtLeast(it.height) / 2f) + 25f
                    addOval(Rect(it.center, radius))
                }
            }
            
            drawPath(
                path = spotlightPath,
                color = Color.Black.copy(alpha = 0.82f),
                style = Fill
            )
            
            targetRect?.let {
                val radius = (it.width.coerceAtLeast(it.height) / 2f) + 25f
                drawCircle(
                    color = Color(0xFF64FFDA),
                    radius = radius,
                    center = it.center,
                    style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f)))
                )
            }
        }

        // --- TARJETA DE TEXTO ---
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = if (targetRect == null || currentTarget == TutorialTarget.SCORE) Alignment.Center else Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier.width(280.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(steps[currentStep].icon, fontSize = 44.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = steps[currentStep].title,
                        color = Color(0xFF1A237E),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = steps[currentStep].description,
                        color = Color(0xFF1A237E).copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        text = "Toca para continuar",
                        color = Color(0xFF1A237E).copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

fun Rect.inflate(amount: Float): Rect = Rect(left - amount, top - amount, right + amount, bottom + amount)
