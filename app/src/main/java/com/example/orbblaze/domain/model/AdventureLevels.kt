package com.example.orbblaze.domain.model

object AdventureLevels {
    val levels = listOf(
        // ==========================================
        // ZONA 1: JUNGLA (Niveles 1-5) - Tutorial
        // ==========================================
        Level(1, AdventureZone.JUNGLE, 15, listOf(
            "  R R  ",
            " B B B ",
            "  R R  "
        ), LevelObjective.ClearBoard(), 500, 1000, 1500),

        Level(2, AdventureZone.JUNGLE, 18, listOf(
            " G G Y Y",
            "G Y Y G ",
            " G G Y Y"
        ), LevelObjective.ReachScore(2000), 1000, 2000, 3000),

        Level(3, AdventureZone.JUNGLE, 20, listOf(
            "P P P P",
            " C C C ",
            "P P P P"
        ), LevelObjective.CollectColor('P', 8, "Recoge 8 Morados"), 1500, 2500, 3500),

        Level(4, AdventureZone.JUNGLE, 22, listOf(
            "R B G Y",
            "Y G B R",
            "R B G Y"
        ), LevelObjective.ClearBoard(), 2000, 3500, 5000),

        Level(5, AdventureZone.JUNGLE, 25, listOf(
            "   B   ",
            "  B R B  ",
            " B R G R B ",
            "B R G Y G R B"
        ), LevelObjective.ReachScore(3000), 2500, 4000, 6000),

        // ==========================================
        // ZONA 2: CUEVA (Niveles 6-10) - Estrecho
        // ==========================================
        Level(6, AdventureZone.CAVE, 20, listOf(
            "P     P",
            " P   P ",
            "  P P  ",
            "   P   "
        ), LevelObjective.ClearBoard(), 2000, 3500, 5000),

        Level(7, AdventureZone.CAVE, 25, listOf(
            "R C R C R",
            " C R C R ",
            "R C R C R"
        ), LevelObjective.CollectColor('C', 10, "Recoge 10 Celestes"), 3000, 5000, 7000),

        Level(8, AdventureZone.CAVE, 28, listOf(
            "Y G G G G Y",
            "Y B B B B Y",
            "Y P P P P Y"
        ), LevelObjective.ReachScore(5000), 4000, 6000, 8000),

        Level(9, AdventureZone.CAVE, 30, listOf(
            "R B G Y P C",
            " C P Y G B R",
            " R B G Y P C"
        ), LevelObjective.ClearBoard(), 3000, 5000, 7000),

        Level(10, AdventureZone.CAVE, 35, listOf(
            "R R R R R R R",
            "B B     B B",
            "G G G G G G G",
            "P P     P P",
            "Y Y Y Y Y Y Y"
        ), LevelObjective.ReachScore(8000, "¡Sal de la cueva! (8000 pts)"), 5000, 8000, 10000),

        // ==========================================
        // ZONA 3: NÚCLEO (Niveles 11-15) - Denso
        // ==========================================
        Level(11, AdventureZone.CORE, 20, listOf(
            "R B G Y R",
            "B G Y R B",
            "G Y R B G",
            "Y R B G Y"
        ), LevelObjective.ReachScore(8000), 5000, 8000, 11000),

        Level(12, AdventureZone.CORE, 18, listOf(
            "  P P P  ",
            " P C C P ",
            "P C   C P",
            " P C C P "
        ), LevelObjective.CollectColor('P', 12), 4500, 7500, 10000),

        Level(13, AdventureZone.CORE, 25, listOf(
            "G G Y Y R R",
            " G Y Y R R ",
            "B B P P C C",
            " B P P C C "
        ), LevelObjective.ClearBoard(), 5500, 8500, 11500),

        Level(14, AdventureZone.CORE, 22, listOf(
            "Y   Y   Y",
            " Y R Y R ",
            "R   R   R",
            " B G B G "
        ), LevelObjective.ReachScore(9000), 5000, 8000, 11000),

        Level(15, AdventureZone.CORE, 30, listOf(
            "C C C C C C",
            " P P P P P ",
            "  Y Y Y Y  ",
            "   G G G   ",
            "    R R    "
        ), LevelObjective.ClearBoard("¡Escapa del Núcleo!"), 7000, 10000, 14000),

        // ==========================================
        // ZONA 4: SUPERFICIE (Niveles 16-20) - Abierto
        // ==========================================
        Level(16, AdventureZone.SURFACE, 20, listOf(
            "R B   G Y",
            " R B G Y ",
            "  C P C  ",
            " Y G B R ",
            "Y G   B R"
        ), LevelObjective.CollectColor('G', 8), 6000, 9000, 12000),

        Level(17, AdventureZone.SURFACE, 15, listOf(
            "P P P P P",
            "R R R R R",
            "Y Y Y Y Y",
            "G G G G G"
        ), LevelObjective.ClearBoard("¡Cielo Abierto!"), 8000, 11000, 15000),

        Level(18, AdventureZone.SURFACE, 28, listOf(
            "C R C R C",
            " B Y B Y ",
            "C R   R C",
            " B Y B Y ",
            "C R C R C"
        ), LevelObjective.ReachScore(12000), 7500, 11000, 14500),

        Level(19, AdventureZone.SURFACE, 25, listOf(
            "   G G   ",
            "  G Y G  ",
            " G Y B G ",
            "G Y B R G"
        ), LevelObjective.CollectColor('Y', 10), 8000, 12000, 16000),

        Level(20, AdventureZone.SURFACE, 30, listOf(
            "R G B Y P",
            " P Y B G R",
            "R G B Y P",
            " P Y B G R"
        ), LevelObjective.ClearBoard(), 9000, 13000, 17000),

        // ==========================================
        // ZONA 5: ATMÓSFERA (Niveles 21-25) - Flotante
        // ==========================================
        Level(21, AdventureZone.SKY, 22, listOf(
            "C   C   C",
            " C P P C ",
            "  P Y P  ",
            " C P P C ",
            "C   C   C"
        ), LevelObjective.ReachScore(10000), 6000, 9000, 12000),

        Level(22, AdventureZone.SKY, 20, listOf(
            "B B B B B B",
            "           ",
            "R R R R R R",
            "           ",
            "G G G G G G"
        ), LevelObjective.ClearBoard(), 7000, 10000, 13000),

        Level(23, AdventureZone.SKY, 25, listOf(
            "Y Y P P",
            " Y P P ",
            "  C C  ",
            " R R R ",
            "R     R"
        ), LevelObjective.CollectColor('C', 4), 5000, 8000, 11000),

        Level(24, AdventureZone.SKY, 28, listOf(
            "   G   ",
            "  R B  ",
            " Y P C ",
            "G R B Y",
            " P C G "
        ), LevelObjective.ReachScore(15000), 8000, 12000, 16000),

        Level(25, AdventureZone.SKY, 30, listOf(
            "C C C C C",
            "C       C",
            "C   P   C",
            "C       C",
            "C C C C C"
        ), LevelObjective.ClearBoard("¡Rompe las nubes!"), 9000, 14000, 18000),

        // ==========================================
        // ZONA 6: GALAXIA (Niveles 26-30) - Experto
        // ==========================================
        Level(26, AdventureZone.SPACE, 20, listOf(
            "R B G Y P C",
            "           ",
            "C P Y G B R",
            "           ",
            "R B G Y P C"
        ), LevelObjective.ReachScore(20000), 10000, 15000, 20000),

        Level(27, AdventureZone.SPACE, 25, listOf(
            "P   P   P",
            " C C C C ",
            "  Y   Y  ",
            " R R R R ",
            "B   B   B"
        ), LevelObjective.CollectColor('P', 6), 11000, 16000, 22000),

        Level(28, AdventureZone.SPACE, 30, listOf(
            "G G G G G G G",
            " R R R R R R ",
            "  Y Y Y Y Y  ",
            "   B B B B   ",
            "    P P P    ",
            "     C C     "
        ), LevelObjective.ClearBoard(), 15000, 20000, 25000),

        Level(29, AdventureZone.SPACE, 15, listOf(
            "R C P Y B G",
            "G B Y P C R",
            "R C P Y B G",
            "G B Y P C R"
        ), LevelObjective.ReachScore(25000, "¡Velocidad luz!"), 12000, 18000, 24000),

        Level(30, AdventureZone.SPACE, 40, listOf( // FINAL BOSS
            "R B G Y P C R",
            " C P Y G B R ",
            "R B G Y P C R",
            " C P Y G B R ",
            "R B G Y P C R",
            " C P Y G B R "
        ), LevelObjective.ClearBoard("¡EL ORIGEN DEL UNIVERSO!"), 20000, 30000, 40000)
    )
}