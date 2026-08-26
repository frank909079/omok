package com.frank.omok.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class LevelConfigTest {

    @Test
    fun `search depth never decreases as level increases`() {
        for (level in 1 until 100) {
            val current = LevelConfig.configForLevel(level)
            val next = LevelConfig.configForLevel(level + 1)
            assertTrue("depth dropped from Lv$level to Lv${level + 1}", next.depth >= current.depth)
        }
    }

    @Test
    fun `blunder rate never increases as level increases`() {
        for (level in 1 until 100) {
            val current = LevelConfig.configForLevel(level)
            val next = LevelConfig.configForLevel(level + 1)
            assertTrue("blunder rate rose from Lv$level to Lv${level + 1}", next.blunderRate <= current.blunderRate)
        }
    }

    @Test
    fun `defense weight never decreases as level increases`() {
        for (level in 1 until 100) {
            val current = LevelConfig.configForLevel(level)
            val next = LevelConfig.configForLevel(level + 1)
            assertTrue("defense weight dropped from Lv$level to Lv${level + 1}", next.defenseWeight >= current.defenseWeight)
        }
    }

    @Test
    fun `level 1 is easiest and level 100 is hardest`() {
        val easiest = LevelConfig.configForLevel(1)
        val hardest = LevelConfig.configForLevel(100)
        assertTrue(hardest.depth > easiest.depth)
        assertTrue(hardest.blunderRate < easiest.blunderRate)
        assertTrue(hardest.defenseWeight > easiest.defenseWeight)
    }

    @Test
    fun `level is clamped to the 1 to 100 range`() {
        val belowRange = LevelConfig.configForLevel(0)
        val atMin = LevelConfig.configForLevel(1)
        assertTrue(belowRange == atMin)

        val aboveRange = LevelConfig.configForLevel(150)
        val atMax = LevelConfig.configForLevel(100)
        assertTrue(aboveRange == atMax)
    }
}
