package com.example.orbblaze.domain.model

object AdventureLevels {
    val levels = listOf(
        // ==========================================
        // ZONA 1: JUNGLA (Niveles 1-5) - Reto: 12-18 tiros
        // ==========================================
        Level(1, AdventureZone.JUNGLE, 12, listOf(
            "  R R G G  ",
            " R R G G B ",
            "  B B Y Y  ",
            " B B Y Y R "
        ), LevelObjective.ClearBoard("Limpia toda la jungla"), 500, 1000, 1500),

        Level(2, AdventureZone.JUNGLE, 12, listOf(
            "P     P",
            " P   P ",
            "  Y Y  ",
            "   G   "
        ), LevelObjective.ClearBoard(), 1000, 2000, 3000),

        Level(3, AdventureZone.JUNGLE, 15, listOf(
            "R B G Y P C",
            " C P Y G B R",
            "R B G Y P C"
        ), LevelObjective.ClearBoard(), 1500, 2500, 3500),

        Level(4, AdventureZone.JUNGLE, 18, listOf(
            "   B   ",
            "  B R B  ",
            " B R G R B ",
            "B R G Y G R B"
        ), LevelObjective.ClearBoard(), 2000, 3500, 5000),

        Level(5, AdventureZone.JUNGLE, 18, listOf(
            "R G B   B G R",
            " R G     G R ",
            "  R       R  "
        ), LevelObjective.ClearBoard(), 2500, 4000, 6000),

        // ==========================================
        // ZONA 2: CUEVA (Niveles 6-10) - Reto: 15-25 tiros
        // ==========================================
        Level(6, AdventureZone.CAVE, 15, listOf(
            "Y Y Y   Y Y Y",
            "Y Y     Y Y",
            "Y         Y"
        ), LevelObjective.ClearBoard(), 2000, 3500, 5000),

        Level(7, AdventureZone.CAVE, 20, listOf(
            "R C R C R C R",
            " C R C R C R ",
            "R C R C R C R"
        ), LevelObjective.ClearBoard(), 3000, 5000, 7000),

        Level(8, AdventureZone.CAVE, 20, listOf(
            "P P P P P P",
            "P         P",
            "P   B B   P",
            "P   B B   P",
            "P         P",
            "P P P P P P"
        ), LevelObjective.ClearBoard(), 4000, 6000, 8000),

        Level(9, AdventureZone.CAVE, 18, listOf(
            "G   G   G",
            " G R G R ",
            "  G R G  ",
            "G   G   G"
        ), LevelObjective.ClearBoard(), 3000, 5000, 7000),

        Level(10, AdventureZone.CAVE, 25, listOf(
            "R R R R R R",
            "B B    B B",
            "G G G G G G",
            "P P    P P",
            "Y Y Y Y Y Y"
        ), LevelObjective.ClearBoard("¡Escapa de la cueva!"), 5000, 8000, 10000),

        // ==========================================
        // ZONA 3: NÚCLEO (Niveles 11-20) - Reto: 15-25 tiros
        // ==========================================
        Level(11, AdventureZone.CORE, 18, listOf(
            "R B G Y P C",
            " B G Y P C ",
            "  G Y P C  ",
            "   Y P C   ",
            "    P C    ",
            "     C     "
        ), LevelObjective.ClearBoard(), 5000, 8000, 11000),

        Level(12, AdventureZone.CORE, 15, listOf(
            "  P P P  ",
            " P C C P ",
            "P C   C P",
            " P C C P ",
            "  P P P  "
        ), LevelObjective.ClearBoard(), 4500, 7500, 10000),

        Level(13, AdventureZone.CORE, 22, listOf(
            "G G Y Y R R B B",
            " G Y Y R R B ",
            "  P P C C    ",
            " P P C C     "
        ), LevelObjective.ClearBoard(), 5500, 8500, 11500),

        Level(14, AdventureZone.CORE, 20, listOf(
            "Y   Y   Y   Y",
            " Y R Y R Y R ",
            "  G B G B G  "
        ), LevelObjective.ClearBoard(), 5000, 8000, 11000),

        Level(15, AdventureZone.CORE, 25, listOf(
            "C C C C C C C C",
            " P P P P P P P ",
            "  Y Y Y Y Y Y  ",
            "   G G G G G   ",
            "    R R R R    "
        ), LevelObjective.ClearBoard(), 7000, 10000, 14000),

        Level(16, AdventureZone.CORE, 22, listOf(
            "R R     R R",
            " R       R ",
            "B B B B B B",
            " G       G ",
            "G G     G G"
        ), LevelObjective.ClearBoard(), 6000, 9000, 12000),

        Level(17, AdventureZone.CORE, 15, listOf(
            "P P P P P P",
            "R R R R R R",
            "Y Y Y Y Y Y",
            "G G G G G G"
        ), LevelObjective.ClearBoard(), 8000, 11000, 15000),

        Level(18, AdventureZone.CORE, 22, listOf(
            "C R C R C R",
            " B Y B Y B ",
            "  C R C R  ",
            "   B Y B   ",
            "    C R    "
        ), LevelObjective.ClearBoard(), 7500, 11000, 14500),

        Level(19, AdventureZone.CORE, 20, listOf(
            "   G G   ",
            "  G Y G  ",
            " G Y B Y G ",
            "G Y B R B Y G"
        ), LevelObjective.ClearBoard(), 8000, 12000, 16000),

        Level(20, AdventureZone.CORE, 25, listOf(
            "R G B Y P",
            " R G B Y P",
            "R G B Y P",
            " R G B Y P",
            "R G B Y P"
        ), LevelObjective.ClearBoard(), 9000, 13000, 17000),

        // ==========================================
        // ZONA 4: SUPERFICIE (Niveles 21-30) - Reto: 15-25 tiros
        // ==========================================
        Level(21, AdventureZone.SURFACE, 20, listOf(
            "C   C   C   C",
            " C P P C P P ",
            "  P Y Y P Y  ",
            "   C P P C   "
        ), LevelObjective.ClearBoard(), 6000, 9000, 12000),

        Level(22, AdventureZone.SURFACE, 15, listOf(
            "B B B B B B",
            "           ",
            "R R R R R R",
            "           ",
            "G G G G G G"
        ), LevelObjective.ClearBoard(), 7000, 10000, 13000),

        Level(23, AdventureZone.SURFACE, 20, listOf(
            "Y Y P P G G",
            " Y P P G ",
            "  C C R R  ",
            "   R R B   ",
            "    B B    "
        ), LevelObjective.ClearBoard(), 5000, 8000, 11000),

        Level(24, AdventureZone.SURFACE, 22, listOf(
            "   G   ",
            "  R B  ",
            " Y P C ",
            "G R B Y",
            " P C G R",
            "  B Y P  "
        ), LevelObjective.ClearBoard(), 8000, 12000, 16000),

        Level(25, AdventureZone.SURFACE, 25, listOf(
            "C C C C C C",
            "C         C",
            "C   P P   C",
            "C   P P   C",
            "C         C",
            "C C C C C C"
        ), LevelObjective.ClearBoard(), 9000, 14000, 18000),

        Level(26, AdventureZone.SURFACE, 20, listOf(
            "G       G",
            " G R R R G ",
            "  R B B R  ",
            " G R R R G ",
            "G       G"
        ), LevelObjective.ClearBoard(), 10000, 15000, 20000),

        Level(27, AdventureZone.SURFACE, 22, listOf(
            "Y Y Y Y Y Y",
            " Y       Y ",
            "  Y     Y  ",
            "   Y   Y   ",
            "    Y Y    ",
            "     Y     "
        ), LevelObjective.ClearBoard(), 11000, 16000, 22000),

        Level(28, AdventureZone.SURFACE, 25, listOf(
            "R B   G Y",
            " R     Y ",
            "  P C P  ",
            " G     B ",
            "G B   P R"
        ), LevelObjective.ClearBoard(), 15000, 20000, 25000),

        Level(29, AdventureZone.SURFACE, 18, listOf(
            "R C P Y B G R",
            " G B Y P C R ",
            "R C P Y B G R",
            " G B Y P C R ",
            "R C P Y B G R"
        ), LevelObjective.ClearBoard(), 12000, 18000, 24000),

        Level(30, AdventureZone.SURFACE, 30, listOf(
            "P  B  G  Y  C",
            " P  B  G  Y  C ",
            "  P  B  G  Y  C",
            " P  B  G  Y  C ",
            "  P  B  G  Y  C"
        ), LevelObjective.ClearBoard(), 20000, 30000, 40000),

        // ==========================================
        // ZONA 5: ATMÓSFERA (Niveles 31-40) - Reto: 20-35 tiros
        // ==========================================
        Level(31, AdventureZone.SKY, 25, listOf(
            "R   R   R",
            " B B B B B ",
            "  G   G  ",
            " P P P P P ",
            "  C   C  "
        ), LevelObjective.ClearBoard(), 14000, 19000, 24000),

        Level(32, AdventureZone.SKY, 22, listOf(
            "Y G R B P C",
            " Y G R B P C",
            "Y G R B P C ",
            " Y G R B P C",
            "Y G R B P C "
        ), LevelObjective.ClearBoard(), 15000, 21000, 28000),

        Level(33, AdventureZone.SKY, 28, listOf(
            "B Y G   G Y B",
            " B Y     Y B ",
            "  B       B  ",
            " C P R R P C ",
            "  C P P C  ",
            "   C   C   "
        ), LevelObjective.ClearBoard(), 16000, 22000, 30000),

        Level(34, AdventureZone.SKY, 20, listOf(
            "R R R   R R R",
            "R R     R R",
            "R         R",
            "G G     G G",
            "G G G   G G G"
        ), LevelObjective.ClearBoard(), 14000, 20000, 27000),

        Level(35, AdventureZone.SKY, 30, listOf(
            "C   P   Y   G",
            " C P Y G B R ",
            "  C P Y G B  ",
            "   C P Y G   ",
            "    C P Y    ",
            "     C P     "
        ), LevelObjective.ClearBoard(), 18000, 25000, 33000),

        Level(36, AdventureZone.SKY, 25, listOf(
            "R B   B R",
            " G P Y P G ",
            "  B C R C B  ",
            "   Y G B Y   ",
            "    P C P    ",
            "     R R     "
        ), LevelObjective.ClearBoard(), 17000, 24000, 32000),

        Level(37, AdventureZone.SKY, 22, listOf(
            "Y Y Y Y Y Y Y",
            "Y           Y",
            "Y G G G G G Y",
            "Y G       G Y",
            "Y G R R R G Y",
            "Y G R   R G Y"
        ), LevelObjective.ClearBoard(), 19000, 26000, 35000),

        Level(38, AdventureZone.SKY, 28, listOf(
            "R   B   G   Y",
            " R B G Y P C ",
            "  R B G Y P  ",
            "   R B G Y   ",
            "    R B G    ",
            "     R B     "
        ), LevelObjective.ClearBoard(), 20000, 28000, 38000),

        Level(39, AdventureZone.SKY, 25, listOf(
            "P P P   P P P",
            " P P     P P ",
            "  P       P  ",
            " B B B B B B B ",
            "  C       C  ",
            " C C     C C ",
            "C C C   C C C"
        ), LevelObjective.ClearBoard(), 22000, 30000, 40000),

        Level(40, AdventureZone.SKY, 35, listOf(
            "R B G   G B R",
            " B G     G B ",
            "  G       G  ",
            " Y P C C P Y ",
            "  P C   C P  ",
            "   C     C   "
        ), LevelObjective.ClearBoard(), 25000, 35000, 45000),

        // ==========================================
        // ZONA 6: GALAXIA (Niveles 41-50) - Reto: 25-45 tiros
        // ==========================================
        Level(41, AdventureZone.SPACE, 30, listOf(
            "  R R   G G  ",
            " R   R G   G ",
            "  B B   P P  ",
            " B   B P   P ",
            "  Y Y   C C  ",
            " Y   Y C   C "
        ), LevelObjective.ClearBoard(), 28000, 38000, 50000),

        Level(42, AdventureZone.SPACE, 25, listOf(
            "R G B Y P C",
            "           ",
            "R G B Y P C",
            "           ",
            "R G B Y P C",
            "           ",
            "R G B Y P C"
        ), LevelObjective.ClearBoard(), 30000, 40000, 55000),

        Level(43, AdventureZone.SPACE, 35, listOf(
            "P           P",
            " P B     B P ",
            "  P B G B P  ",
            "   P B G B P   ",
            "  P B G B P  ",
            " P B     B P ",
            "P           P"
        ), LevelObjective.ClearBoard(), 32000, 42000, 58000),

        Level(44, AdventureZone.SPACE, 28, listOf(
            "C Y G B R P",
            " C Y G B R",
            "  C Y G B",
            "   C Y G",
            "    C Y",
            "     C"
        ), LevelObjective.ClearBoard(), 30000, 40000, 55000),

        Level(45, AdventureZone.SPACE, 35, listOf(
            "R B G   G B R",
            " B G     G B ",
            "G R B   B R G",
            " R B     B R ",
            "B G R   R G B",
            " G R     R G "
        ), LevelObjective.ClearBoard(), 35000, 48000, 62000),

        Level(46, AdventureZone.SPACE, 30, listOf(
            "R G R G R G R",
            "R G R G R G R",
            "B P B P B P B",
            "B P B P B P B",
            "Y C Y C Y C Y",
            "Y C Y C Y C Y"
        ), LevelObjective.ClearBoard(), 38000, 50000, 65000),

        Level(47, AdventureZone.SPACE, 40, listOf(
            "Y           Y",
            " Y    B    Y ",
            "  Y   B   Y  ",
            "   Y  B  Y   ",
            "  Y   B   Y  ",
            " Y    B    Y ",
            "Y           Y"
        ), LevelObjective.ClearBoard(), 40000, 55000, 70000),

        Level(48, AdventureZone.SPACE, 35, listOf(
            "R B   Y P   R B",
            " R B Y P C R B ",
            "  R B Y P C R  ",
            "   R B Y P C   ",
            "    R B Y P    ",
            "     R B Y     "
        ), LevelObjective.ClearBoard(), 42000, 58000, 75000),

        Level(49, AdventureZone.SPACE, 40, listOf(
            "G G G   G G G",
            "G     C     G",
            "G   B   B   G",
            "G C   R   C G",
            "G   B   B   G",
            "G     C     G",
            "G G G   G G G"
        ), LevelObjective.ClearBoard(), 45000, 60000, 80000),

        Level(50, AdventureZone.SPACE, 45, listOf(
            "R G B Y P C R",
            "R G B Y P C R",
            "R G B Y P C R",
            "R G B Y P C R",
            "R G B Y P C R",
            "R G B Y P C R",
            "R G B Y P C R"
        ), LevelObjective.ClearBoard("¡CONQUISTA EL COSMOS!"), 50000, 75000, 100000)
    )
}
