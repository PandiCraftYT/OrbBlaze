package com.example.orbblaze.domain.model

object AdventureLevels {
    val levels = listOf(
        AdventureLevel(
            id = 1,
            zone = AdventureZone.CORE,
            layout = listOf(
                "  R R R R  ",
                "   B B B   ",
                "    G G    "
            ),
            maxShots = 12,
            targetScore = 500
        ),
        AdventureLevel(
            id = 2,
            zone = AdventureZone.CORE,
            layout = listOf(
                " P P P P P ",
                "  C C C C  ",
                "   Y Y Y   ",
                "    B B    "
            ),
            maxShots = 15,
            targetScore = 800
        ),
        // Puedes seguir añadiendo niveles aquí siguiendo este patrón
        AdventureLevel(
            id = 11,
            zone = AdventureZone.SURFACE,
            layout = listOf(
                " G G G G G ",
                "  Y Y Y Y  ",
                "   R R R   "
            ),
            maxShots = 10,
            targetScore = 1000
        )
    )
}
