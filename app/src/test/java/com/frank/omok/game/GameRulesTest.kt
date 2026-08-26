package com.frank.omok.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {

    private fun boardWith(vararg points: Pair<Point, Stone>): Board {
        var board = Board()
        for ((point, stone) in points) {
            board = board.place(point.row, point.col, stone)
        }
        return board
    }

    @Test
    fun `black exactly five in a row wins for black`() {
        val board = boardWith(
            Point(7, 3) to Stone.BLACK,
            Point(7, 4) to Stone.BLACK,
            Point(7, 5) to Stone.BLACK,
            Point(7, 6) to Stone.BLACK,
            Point(7, 7) to Stone.BLACK
        )
        val result = GameRules.evaluateMove(board, Point(7, 7), Stone.BLACK)
        assertTrue(result is GameResult.Win)
        val win = result as GameResult.Win
        assertEquals(Stone.BLACK, win.winner)
        assertEquals(WinReason.FIVE_IN_A_ROW, win.reason)
    }

    @Test
    fun `black six in a row is an overline loss for black`() {
        val board = boardWith(
            Point(10, 0) to Stone.BLACK,
            Point(10, 1) to Stone.BLACK,
            Point(10, 2) to Stone.BLACK,
            Point(10, 3) to Stone.BLACK,
            Point(10, 4) to Stone.BLACK,
            Point(10, 5) to Stone.BLACK
        )
        val result = GameRules.evaluateMove(board, Point(10, 5), Stone.BLACK)
        assertTrue(result is GameResult.Win)
        val win = result as GameResult.Win
        assertEquals(Stone.WHITE, win.winner)
        assertEquals(WinReason.OVERLINE, win.reason)
    }

    @Test
    fun `white six in a row is a normal win, unrestricted`() {
        val board = boardWith(
            Point(2, 0) to Stone.WHITE,
            Point(2, 1) to Stone.WHITE,
            Point(2, 2) to Stone.WHITE,
            Point(2, 3) to Stone.WHITE,
            Point(2, 4) to Stone.WHITE,
            Point(2, 5) to Stone.WHITE
        )
        val result = GameRules.evaluateMove(board, Point(2, 5), Stone.WHITE)
        assertTrue(result is GameResult.Win)
        val win = result as GameResult.Win
        assertEquals(Stone.WHITE, win.winner)
        assertEquals(WinReason.FIVE_IN_A_ROW, win.reason)
    }

    @Test
    fun `no line and board not full stays playing`() {
        val board = boardWith(Point(5, 5) to Stone.BLACK)
        val result = GameRules.evaluateMove(board, Point(5, 5), Stone.BLACK)
        assertEquals(GameResult.Playing, result)
    }
}
