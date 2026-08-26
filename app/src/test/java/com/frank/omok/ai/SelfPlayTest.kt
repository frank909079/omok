package com.frank.omok.ai

import com.frank.omok.game.Board
import com.frank.omok.game.GameResult
import com.frank.omok.game.GameRules
import com.frank.omok.game.Stone
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfPlayTest {

    private fun playGame(blackLevel: Int, whiteLevel: Int): Stone? {
        var board = Board()
        var turn = Stone.BLACK

        while (true) {
            val level = if (turn == Stone.BLACK) blackLevel else whiteLevel
            val opponent = if (turn == Stone.BLACK) Stone.WHITE else Stone.BLACK
            val move = Ai.chooseMoveForLevel(board, turn, opponent, level, timeBudgetMillis = 150L)
            board = board.place(move.row, move.col, turn)
            val result = GameRules.evaluateMove(board, move, turn)
            if (result is GameResult.Win) return result.winner
            if (result is GameResult.Draw) return null
            turn = opponent
        }
    }

    @Test
    fun `a much stronger level wins more self-play games than a much weaker one`() {
        val strongLevel = 60
        val weakLevel = 5
        var strongWins = 0
        var weakWins = 0
        val gamesPerColor = 6

        repeat(gamesPerColor) {
            when (playGame(blackLevel = strongLevel, whiteLevel = weakLevel)) {
                Stone.BLACK -> strongWins++
                Stone.WHITE -> weakWins++
                else -> {}
            }
        }
        repeat(gamesPerColor) {
            when (playGame(blackLevel = weakLevel, whiteLevel = strongLevel)) {
                Stone.BLACK -> weakWins++
                Stone.WHITE -> strongWins++
                else -> {}
            }
        }

        assertTrue(
            "expected Lv$strongLevel ($strongWins wins) to beat Lv$weakLevel ($weakWins wins) more often",
            strongWins > weakWins
        )
    }
}
