package com.example.orbblaze.domain.model

object GameConstants {
    // Física de alta precisión
    const val PROJECTILE_SPEED = 2800f 
    const val PHYSICS_STEPS = 8 // Pasos de sub-simulación para colisiones perfectas
    
    // Optimización de Partículas y Efectos
    const val MAX_PARTICLES = 100 
    const val PARTICLE_GRAVITY = 180f
    const val PARTICLE_LIFE_DECAY = 3.0f
    const val TEXT_FLOAT_SPEED = 120f
    const val TEXT_LIFE_DECAY = 1.5f
    
    // Mecánicas de Juego (Ajuste de sensibilidad)
    const val BUBBLE_COLLISION_SCALE = 0.85f 
    const val MAGNETIC_BIAS_LOW = 0.2f
    const val MAGNETIC_BIAS_HIGH = 4.5f
    
    // Tiempos y Penalizaciones
    const val TIME_ATTACK_INITIAL = 90
    const val TIME_ATTACK_PENALTY_ROWS = 3
}
