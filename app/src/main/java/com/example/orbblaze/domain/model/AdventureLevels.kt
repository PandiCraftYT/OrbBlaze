package com.example.orbblaze.domain.model

object AdventureLevels {
    val levels = listOf(
        // --- ZONA 1: JUNGLA DE BAMBÚ ---
        Level(1, AdventureZone.JUNGLE, 15, listOf(
            "  R R  ",
            " B B B ",
            "  R R  "
        )),
        Level(2, AdventureZone.JUNGLE, 20, listOf(
            " G G Y Y",
            "G Y Y G ",
            " G G Y Y"
        )),
        Level(3, AdventureZone.JUNGLE, 18, listOf(
            "P P P P",
            " C C C ",
            "P P P P",
            " C C C "
        )),
        Level(4, AdventureZone.JUNGLE, 22, listOf(
            "R B G Y",
            "Y G B R",
            "R B G Y",
            "Y G B R"
        )),
        Level(5, AdventureZone.JUNGLE, 25, listOf(
            "   B   ",
            "  B R B  ",
            " B R G R B ",
            "B R G Y G R B"
        )),

        // --- ZONA 2: CUEVA CRISTALINA ---
        Level(6, AdventureZone.CAVE, 20, listOf(
            "P     P",
            " P   P ",
            "  P P  ",
            "   P   "
        )),
        Level(7, AdventureZone.CAVE, 25, listOf(
            "R C R C R",
            " C R C R ",
            "R C R C R"
        )),
        Level(8, AdventureZone.CAVE, 28, listOf(
            "Y G G G G Y",
            "Y B B B B Y",
            "Y P P P P Y"
        )),
        Level(9, AdventureZone.CAVE, 30, listOf(
            "R B G Y P C",
            " C P Y G B R",
            " R B G Y P C",
            "  C P Y G B R "
        )),
        Level(10, AdventureZone.CAVE, 35, listOf(
            "R R R R R R R",
            "B B     B B",
            "G G G G G G G",
            "P P     P P",
            "Y Y Y Y Y Y Y"
        ))
    )
}
