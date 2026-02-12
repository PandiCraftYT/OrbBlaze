package com.example.orbblaze.domain.model

object GameConstants {
    // Física y Movimiento
    const val PROJECTILE_SPEED = 2500f // Aumentado para mayor velocidad
    const val PHYSICS_STEPS = 5
    
    // Partículas
    const val PARTICLE_GRAVITY = 150f
    const val PARTICLE_LIFE_DECAY = 2.5f
    const val TEXT_FLOAT_SPEED = 120f
    const val TEXT_LIFE_DECAY = 1.5f
    
    // Juego
    const val BUBBLE_COLLISION_SCALE = 0.82f
    const val MAGNETIC_BIAS_LOW = 0.25f
    const val MAGNETIC_BIAS_HIGH = 5.0f
    
    // Tiempos
    const val TIME_ATTACK_INITIAL = 90
    const val TIME_ATTACK_PENALTY_ROWS = 3
}
