package com.example.orbblaze.domain.model

object AdventureLevels {
    val levels = listOf(
        // ==========================================
        // ZONA 1: JUNGLA (1-15) - Tutoriales (Sin Estrellas)
        // ==========================================
        Level(1, AdventureZone.JUNGLE, 15, listOf("  R B G Y  ", " B G Y R B ", "  G Y R B  "), LevelObjective.ClearBoard(), 500, 1000, 1500),
        Level(2, AdventureZone.JUNGLE, 15, listOf("P B G Y C R", " B G Y C R P"), LevelObjective.ClearBoard(), 1000, 2000, 3000),
        Level(3, AdventureZone.JUNGLE, 18, listOf("R B G Y P C", " C P Y G B R"), LevelObjective.ClearBoard(), 1500, 2500, 3500),
        Level(4, AdventureZone.JUNGLE, 20, listOf("R B G Y P C", " B G Y P C R"), LevelObjective.ClearBoard(), 2000, 3500, 5000),
        Level(5, AdventureZone.JUNGLE, 20, listOf("R G B Y P C", " C P Y G B R"), LevelObjective.ClearBoard(), 2500, 4000, 6000),
        Level(6, AdventureZone.JUNGLE, 18, listOf("Y R G B P C", " R G B P C Y"), LevelObjective.ClearBoard(), 2000, 3500, 5000),
        Level(7, AdventureZone.JUNGLE, 22, listOf("R C R C R C R", " C R C R C R "), LevelObjective.ClearBoard(), 3000, 5000, 7000),
        Level(8, AdventureZone.JUNGLE, 22, listOf("P B G Y P C", "B G Y P C R"), LevelObjective.ClearBoard(), 4000, 6000, 8000),
        Level(9, AdventureZone.JUNGLE, 20, listOf("G R B Y P C", " R B Y P C G"), LevelObjective.ClearBoard(), 3000, 5000, 7000),
        Level(10, AdventureZone.JUNGLE, 28, listOf("R R B B G G", " B B G G R R"), LevelObjective.ClearBoard(), 5000, 8000, 10000),
        Level(11, AdventureZone.JUNGLE, 20, listOf("R B G Y P C", " B G Y P C "), LevelObjective.ClearBoard(), 5000, 8000, 11000),
        Level(12, AdventureZone.JUNGLE, 18, listOf("P B G Y C R", " B G Y C R P"), LevelObjective.ClearBoard(), 4500, 7500, 10000),
        Level(13, AdventureZone.JUNGLE, 25, listOf("G Y R B G Y", " Y R B G Y R"), LevelObjective.ClearBoard(), 5500, 8500, 11500),
        Level(14, AdventureZone.JUNGLE, 22, listOf("Y R B G Y R", " R B G Y R B"), LevelObjective.ClearBoard(), 5000, 8000, 11000),
        Level(15, AdventureZone.JUNGLE, 28, listOf("C P Y G B R C", " P Y G B R C P"), LevelObjective.ClearBoard(), 7000, 10000, 14000),

        // ==========================================
        // ZONA 2: CUEVA (16-30) - Retos Estáticos (Sin Estrellas)
        // ==========================================
        Level(16, AdventureZone.CAVE, 25, listOf("R B G Y P C", " B G Y P C R", " G Y P C R B"), LevelObjective.ClearBoard(), 6000, 9000, 12000),
        Level(17, AdventureZone.CAVE, 18, listOf("P C Y G B R", " C Y G B R P", " Y G B R P C"), LevelObjective.ClearBoard(), 8000, 11000, 15000),
        Level(18, AdventureZone.CAVE, 25, listOf("C R B Y P G", " R B Y P G C"), LevelObjective.ClearBoard(), 7500, 11000, 14500),
        Level(19, AdventureZone.CAVE, 22, listOf("R G B Y P C", " C P Y G B R"), LevelObjective.ClearBoard(), 8000, 12000, 16000),
        Level(20, AdventureZone.CAVE, 28, listOf("R G B Y P C", " G B Y P C R"), LevelObjective.ClearBoard(), 9000, 13000, 17000),
        Level(21, AdventureZone.CAVE, 22, listOf("C P Y G B R", " P Y G B R C"), LevelObjective.ClearBoard(), 6000, 9000, 12000),
        Level(22, AdventureZone.CAVE, 18, listOf("B G Y P C R", " R B G Y P C"), LevelObjective.ClearBoard(), 7000, 10000, 13000),
        Level(23, AdventureZone.CAVE, 25, listOf("Y P G C R B", " P G C R B Y"), LevelObjective.ClearBoard(), 5000, 8000, 11000),
        Level(24, AdventureZone.CAVE, 25, listOf("R B G Y P C", " G Y P C R B"), LevelObjective.ClearBoard(), 8000, 12000, 16000),
        Level(25, AdventureZone.CAVE, 30, listOf("C R B Y P G", " R B Y P G C"), LevelObjective.ClearBoard(), 9000, 14000, 18000),
        Level(26, AdventureZone.CAVE, 25, listOf("G Y P C R B", " Y P C R B G"), LevelObjective.ClearBoard(), 10000, 15000, 20000),
        Level(27, AdventureZone.CAVE, 25, listOf("Y R B G P C", " R B G P C Y"), LevelObjective.ClearBoard(), 11000, 16000, 22000),
        Level(28, AdventureZone.CAVE, 28, listOf("R G B Y P C", " G B Y P C R"), LevelObjective.ClearBoard(), 15000, 20000, 25000),
        Level(29, AdventureZone.CAVE, 20, listOf("R C P Y B G R", " G B Y P C R "), LevelObjective.ClearBoard(), 12000, 18000, 24000),
        Level(30, AdventureZone.CAVE, 35, listOf("P B G Y P C R", " B G Y P C R B"), LevelObjective.ClearBoard(), 20000, 30000, 40000),

        // ==========================================
        // ZONA 3: NÚCLEO (31-50) - 🌊 MODO CASCADA COMPACTO
        // ==========================================
        Level(31, AdventureZone.CORE, 25, listOf("RRBBGGYYPP", "BBGGYYPPRR"), LevelObjective.ReachScore(800), 500, 700, 800),
        Level(32, AdventureZone.CORE, 25, listOf("RRBBGGYYRR", "BBGGYYRRBB"), LevelObjective.ReachScore(900), 600, 800, 900),
        Level(33, AdventureZone.CORE, 28, listOf("PPPCCCBBBR", "CCCBBBRRPP"), LevelObjective.ReachScore(1000), 700, 900, 1000),
        Level(34, AdventureZone.CORE, 25, listOf("RRGGBBYYCC", "GGBBYYCCRR"), LevelObjective.ReachScore(850), 550, 750, 850),
        Level(35, AdventureZone.CORE, 30, listOf("RRRBBBYYYG", "BBBYYYGRRR"), LevelObjective.ReachScore(1100), 800, 1000, 1100),
        Level(36, AdventureZone.CORE, 30, listOf("CCCCCYYYYY", "YYYYYCCCCC"), LevelObjective.ReachScore(1200), 900, 1100, 1200),
        Level(37, AdventureZone.CORE, 32, listOf("RRRRRGGGGG", "BBBBBYYYYY"), LevelObjective.ReachScore(1300), 1000, 1200, 1300),
        Level(38, AdventureZone.CORE, 30, listOf("BBBGGGYYYR", "GGGYYYRRRB"), LevelObjective.ReachScore(1250), 900, 1150, 1250),
        Level(39, AdventureZone.CORE, 35, listOf("RBRBRBRBRB", "GYGYGYGYGY"), LevelObjective.ReachScore(1500), 1100, 1350, 1500),
        Level(40, AdventureZone.CORE, 32, listOf("RRGGBBYYCC", "GGBBYYCCRR"), LevelObjective.ReachScore(1400), 1000, 1250, 1400),
        Level(41, AdventureZone.CORE, 35, listOf("RRBBGGYYRR", "BBGGYYRRBB"), LevelObjective.ReachScore(1600), 1200, 1450, 1600),
        Level(42, AdventureZone.CORE, 35, listOf("PPPPCCCCBB", "BBPPPPCCCC"), LevelObjective.ReachScore(1550), 1150, 1400, 1550),
        Level(43, AdventureZone.CORE, 38, listOf("RGBRGBRGBR", "GBRGBRGBRG"), LevelObjective.ReachScore(1800), 1300, 1600, 1800),
        Level(44, AdventureZone.CORE, 35, listOf("YYYYYRRRRR", "RRRRRYYYYY"), LevelObjective.ReachScore(1700), 1250, 1550, 1700),
        Level(45, AdventureZone.CORE, 40, listOf("CCCCCCCCCC", "GGGGGGGGGG"), LevelObjective.ReachScore(2000), 1500, 1800, 2000),
        Level(46, AdventureZone.CORE, 38, listOf("RBRBRBRBRB", "GYGYGYGYGY"), LevelObjective.ReachScore(1900), 1400, 1700, 1900),
        Level(47, AdventureZone.CORE, 42, listOf("BBBBBRRRRR", "GGGGGYYYYY"), LevelObjective.ReachScore(2200), 1600, 2000, 2200),
        Level(48, AdventureZone.CORE, 40, listOf("PPPPPCCCCC", "YYYYYRRRRR"), LevelObjective.ReachScore(2100), 1500, 1900, 2100),
        Level(49, AdventureZone.CORE, 45, listOf("CCCCCPPPPP", "RRRRRBBBBB"), LevelObjective.ReachScore(2500), 1800, 2200, 2500),
        Level(50, AdventureZone.CORE, 42, listOf("RRRRRGGGGG", "BBBBBYYYYY"), LevelObjective.ReachScore(2300), 1700, 2100, 2300),

        // ==========================================
        // ZONA 4: SURFACE (51-70) - Supervivencia Compacta
        // ==========================================
        Level(51, AdventureZone.SURFACE, 40, listOf("BBBBBYYYYY", "RRRRRCCCCC"), LevelObjective.ReachScore(3000), 2000, 2500, 3000),
        Level(52, AdventureZone.SURFACE, 38, listOf("RRGGBBYYCC", "GGBBYYCCRR"), LevelObjective.ReachScore(2800), 1800, 2400, 2800),
        Level(53, AdventureZone.SURFACE, 42, listOf("RRRRRBBBBB", "BBBBBRRRRR"), LevelObjective.ReachScore(3200), 2200, 2800, 3200),
        Level(54, AdventureZone.SURFACE, 40, listOf("GGGGYYYYYY", "YYYYGGGGGG"), LevelObjective.ReachScore(3100), 2100, 2700, 3100),
        Level(55, AdventureZone.SURFACE, 45, listOf("PPPPCCCCBB", "CCCCBBPPPP"), LevelObjective.ReachScore(3500), 2500, 3100, 3500),
        Level(56, AdventureZone.SURFACE, 42, listOf("RRRGGGBBBY", "GGGBBBYRRR"), LevelObjective.ReachScore(3300), 2300, 2900, 3300),
        Level(57, AdventureZone.SURFACE, 48, listOf("PPPCCCBBBR", "CCCBBBRRPP"), LevelObjective.ReachScore(4000), 3000, 3600, 4000),
        Level(58, AdventureZone.SURFACE, 45, listOf("BBBBBBBBBB", "GGGGGGGGGG"), LevelObjective.ReachScore(3800), 2800, 3400, 3800),
        Level(59, AdventureZone.SURFACE, 50, listOf("RRRRRBBBBB", "CCCCCYYYYY"), LevelObjective.ReachScore(4500), 3500, 4100, 4500),
        Level(60, AdventureZone.SURFACE, 48, listOf("PPPPPCCCCC", "GGGGGYYYYY"), LevelObjective.ReachScore(4300), 3300, 3900, 4300),
        Level(61, AdventureZone.SURFACE, 52, listOf("RBRBRBRBRB", "GYGYGYGYGY"), LevelObjective.ReachScore(5000), 4000, 4600, 5000),
        Level(62, AdventureZone.SURFACE, 50, listOf("CCCCBBBBBB", "RRRRRGGGGG"), LevelObjective.ReachScore(4800), 3800, 4400, 4800),
        Level(63, AdventureZone.SURFACE, 55, listOf("RRGGBBYYCC", "GGBBYYCCRR"), LevelObjective.ReachScore(5500), 4500, 5100, 5500),
        Level(64, AdventureZone.SURFACE, 52, listOf("BBBBBBBBBB", "RRRRRRRRRR"), LevelObjective.ReachScore(5300), 4300, 4900, 5300),
        Level(65, AdventureZone.SURFACE, 58, listOf("GGGGGGGGGG", "YYYYYYYYYY"), LevelObjective.ReachScore(6000), 5000, 5600, 6000),
        Level(66, AdventureZone.SURFACE, 55, listOf("PPPPPPCCCC", "RRRRRRGGGG"), LevelObjective.ReachScore(5800), 4800, 5400, 5800),
        Level(67, AdventureZone.SURFACE, 60, listOf("CCCCCPPPPP", "RRRRRBBBBB"), LevelObjective.ReachScore(6500), 5500, 6100, 6500),
        Level(68, AdventureZone.SURFACE, 58, listOf("BBBBBBBBBB", "GGGGGGGGGG"), LevelObjective.ReachScore(6300), 5300, 5900, 6300),
        Level(69, AdventureZone.SURFACE, 65, listOf("RRRRRBBBBB", "CCCCCYYYYY"), LevelObjective.ReachScore(7000), 6000, 6600, 7000),
        Level(70, AdventureZone.SURFACE, 62, listOf("PPPPPCCCCC", "GGGGGYYYYY"), LevelObjective.ReachScore(6800), 5800, 6400, 6800),

        // ==========================================
        // ZONA 5: SKY (71-90) - Patrones Densos
        // ==========================================
        Level(71, AdventureZone.SKY, 50, listOf("RRBBGGYYPP", "BBGGYYPPRR"), LevelObjective.ReachScore(8000), 6000, 7000, 8000),
        Level(72, AdventureZone.SKY, 48, listOf("PPPPPPPPPP", "CCCCCCCCCC"), LevelObjective.ReachScore(7500), 5500, 6500, 7500),
        Level(73, AdventureZone.SKY, 55, listOf("CCCCBBBBBB", "RRRRRGGGGG"), LevelObjective.ReachScore(9000), 7000, 8000, 9000),
        Level(74, AdventureZone.SKY, 52, listOf("PPPPPYYYYY", "BBBBBCCCCC"), LevelObjective.ReachScore(8500), 6500, 7500, 8500),
        Level(75, AdventureZone.SKY, 58, listOf("RGBYPCRGBY", "CPYBGRCPYB"), LevelObjective.ReachScore(10000), 8000, 9000, 10000),
        Level(76, AdventureZone.SKY, 55, listOf("RRRRRGGGGG", "BBBBBYYYYY"), LevelObjective.ReachScore(9500), 7500, 8500, 9500),
        Level(77, AdventureZone.SKY, 60, listOf("CCCCCPPPPP", "RRRRRBBBBB"), LevelObjective.ReachScore(11000), 9000, 10000, 11000),
        Level(78, AdventureZone.SKY, 58, listOf("YYYYYGGGGG", "BBBBBCCCCC"), LevelObjective.ReachScore(10500), 8500, 9500, 10500),
        Level(79, AdventureZone.SKY, 65, listOf("RGBYPCRGBY", "CPYBGRCPYB"), LevelObjective.ReachScore(12000), 10000, 11000, 12000),
        Level(80, AdventureZone.SKY, 62, listOf("BBBBBRRRRR", "GGGGGPPPPP"), LevelObjective.ReachScore(11500), 9500, 10500, 11500),
        Level(81, AdventureZone.SKY, 68, listOf("RRRRRCCCCC", "YYYYYBBBBB"), LevelObjective.ReachScore(13000), 11000, 12000, 13000),
        Level(82, AdventureZone.SKY, 65, listOf("GGGGGPPPPP", "BBBBBRRRRR"), LevelObjective.ReachScore(12500), 10500, 11500, 12500),
        Level(83, AdventureZone.SKY, 70, listOf("CCCCCPPPPP", "YYYYYRRRRR"), LevelObjective.ReachScore(14000), 12000, 13000, 14000),
        Level(84, AdventureZone.SKY, 68, listOf("RRRRRBBBBB", "GGGGGCCCCC"), LevelObjective.ReachScore(13500), 11500, 12500, 13500),
        Level(85, AdventureZone.SKY, 75, listOf("RGBYPCRGBY", "CPYBGRCPYB"), LevelObjective.ReachScore(15000), 13000, 14000, 15000),
        Level(86, AdventureZone.SKY, 72, listOf("PPPPPBBBBB", "RRRRRYYYYY"), LevelObjective.ReachScore(14500), 12500, 13500, 14500),
        Level(87, AdventureZone.SKY, 78, listOf("CCCCCPPPPP", "GGGGGRRRRR"), LevelObjective.ReachScore(16000), 14000, 15000, 16000),
        Level(88, AdventureZone.SKY, 75, listOf("BBBBBYYYYY", "RRRRRCCCCC"), LevelObjective.ReachScore(15500), 13500, 14500, 15500),
        Level(89, AdventureZone.SKY, 80, listOf("RRRRRBBBBB", "GGGGGYYYYY"), LevelObjective.ReachScore(17000), 15000, 16000, 17000),
        Level(90, AdventureZone.SKY, 78, listOf("PPPPPCCCCC", "GGGGGBBBBB"), LevelObjective.ReachScore(16500), 14500, 15500, 16500),

        // ==========================================
        // ZONA 6: SPACE (91-100) - Finales Épicos
        // ==========================================
        Level(91, AdventureZone.SPACE, 60, listOf("RRRRRBBBBB", "RRRRRBBBBB"), LevelObjective.ReachScore(18000), 15000, 16500, 18000),
        Level(92, AdventureZone.SPACE, 60, listOf("GGGGGYYYYY", "GGGGGYYYYY"), LevelObjective.ReachScore(18000), 15000, 16500, 18000),
        Level(93, AdventureZone.SPACE, 65, listOf("PPPPPPPPPP", "CCCCCCCCCC"), LevelObjective.ReachScore(20000), 17000, 18500, 20000),
        Level(94, AdventureZone.SPACE, 65, listOf("RRRRRRRRRR", "GGGGGGGGGG"), LevelObjective.ReachScore(20000), 17000, 18500, 20000),
        Level(95, AdventureZone.SPACE, 70, listOf("BBBBBBBBBB", "CCCCCCCCCC"), LevelObjective.ReachScore(22000), 19000, 20500, 22000),
        Level(96, AdventureZone.SPACE, 70, listOf("RRRRRBBBBB", "GGGGGYYYYY"), LevelObjective.ReachScore(22000), 19000, 20500, 22000),
        Level(97, AdventureZone.SPACE, 75, listOf("BBBBBBBBBB", "RRRRRRRRRR"), LevelObjective.ReachScore(25000), 21000, 23000, 25000),
        Level(98, AdventureZone.SPACE, 75, listOf("YYYYYYYYYY", "CCCCCCCCCC"), LevelObjective.ReachScore(25000), 21000, 23000, 25000),
        Level(99, AdventureZone.SPACE, 80, listOf("RRRRRBBBBB", "GGGGGYYYYY"), LevelObjective.ReachScore(28000), 24000, 26000, 28000),
        Level(100, AdventureZone.SPACE, 100, listOf("RGBYPCRGBY", "CPYBGRCPYB", "RGBYPCRGBY", "CPYBGRCPYB"), LevelObjective.ReachScore(50000, "¡CONQUISTA EL COSMOS!"), 40000, 45000, 50000)
    )
}
