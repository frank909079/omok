package com.frank.omok.ai

data class AiConfig(
    val depth: Int,
    val blunderRate: Double,
    val defenseWeight: Double
)

object LevelConfig {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 100

    private const val DEFENSE_WEIGHT_MIN = 0.5
    private const val DEFENSE_WEIGHT_MAX = 1.6

    fun configForLevel(level: Int): AiConfig {
        val clamped = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        return AiConfig(
            depth = depthForLevel(clamped),
            blunderRate = blunderRateForLevel(clamped),
            defenseWeight = lerp(
                DEFENSE_WEIGHT_MIN,
                DEFENSE_WEIGHT_MAX,
                (clamped - MIN_LEVEL).toDouble() / (MAX_LEVEL - MIN_LEVEL)
            )
        )
    }

    private fun depthForLevel(level: Int): Int = when {
        level <= 8 -> 0
        level <= 16 -> 1
        level <= 24 -> 2
        level <= 35 -> 3
        level <= 55 -> 4
        level <= 75 -> 5
        else -> 6
    }

    private fun blunderRateForLevel(level: Int): Double = when {
        level <= 8 -> lerp(0.45, 0.25, tierProgress(level, 1, 8))
        level <= 16 -> lerp(0.25, 0.10, tierProgress(level, 9, 16))
        level <= 24 -> lerp(0.10, 0.00, tierProgress(level, 17, 24))
        else -> 0.00
    }

    private fun tierProgress(level: Int, tierStart: Int, tierEnd: Int): Double {
        if (tierEnd == tierStart) return 0.0
        return (level - tierStart).toDouble() / (tierEnd - tierStart)
    }

    private fun lerp(from: Double, to: Double, t: Double): Double = from * (1.0 - t) + to * t
}
