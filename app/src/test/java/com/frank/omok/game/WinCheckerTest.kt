package com.frank.omok.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WinCheckerTest {

    private fun boardWith(vararg points: Pair<Point, Stone>): Board {
        var board = Board()
        for ((point, stone) in points) {
            board = board.place(point.row, point.col, stone)
        }
        return board
    }

    @Test
    fun `five in a row horizontally wins`() {
        val board = boardWith(
            Point(7, 3) to Stone.BLACK,
            Point(7, 4) to Stone.BLACK,
            Point(7, 5) to Stone.BLACK,
            Point(7, 6) to Stone.BLACK,
            Point(7, 7) to Stone.BLACK
        )
        val line = WinChecker.checkWin(board, Point(7, 7))
        assertNotNull(line)
        assertEquals(5, line!!.size)
    }

    @Test
    fun `five in a row vertically wins`() {
        val board = boardWith(
            Point(2, 4) to Stone.WHITE,
            Point(3, 4) to Stone.WHITE,
            Point(4, 4) to Stone.WHITE,
            Point(5, 4) to Stone.WHITE,
            Point(6, 4) to Stone.WHITE
        )
        val line = WinChecker.checkWin(board, Point(2, 4))
        assertNotNull(line)
        assertEquals(5, line!!.size)
    }

    @Test
    fun `five in a row on down-right diagonal wins`() {
        val board = boardWith(
            Point(0, 0) to Stone.BLACK,
            Point(1, 1) to Stone.BLACK,
            Point(2, 2) to Stone.BLACK,
            Point(3, 3) to Stone.BLACK,
            Point(4, 4) to Stone.BLACK
        )
        val line = WinChecker.checkWin(board, Point(4, 4))
        assertNotNull(line)
        assertEquals(5, line!!.size)
    }

    @Test
    fun `five in a row on down-left diagonal wins`() {
        val board = boardWith(
            Point(0, 14) to Stone.WHITE,
            Point(1, 13) to Stone.WHITE,
            Point(2, 12) to Stone.WHITE,
            Point(3, 11) to Stone.WHITE,
            Point(4, 10) to Stone.WHITE
        )
        val line = WinChecker.checkWin(board, Point(0, 14))
        assertNotNull(line)
        assertEquals(5, line!!.size)
    }

    @Test
    fun `six in a row still wins under free rule`() {
        val board = boardWith(
            Point(10, 0) to Stone.BLACK,
            Point(10, 1) to Stone.BLACK,
            Point(10, 2) to Stone.BLACK,
            Point(10, 3) to Stone.BLACK,
            Point(10, 4) to Stone.BLACK,
            Point(10, 5) to Stone.BLACK
        )
        val line = WinChecker.checkWin(board, Point(10, 5))
        assertNotNull(line)
        assertEquals(6, line!!.size)
    }

    @Test
    fun `four in a row is not a win`() {
        val board = boardWith(
            Point(7, 3) to Stone.BLACK,
            Point(7, 4) to Stone.BLACK,
            Point(7, 5) to Stone.BLACK,
            Point(7, 6) to Stone.BLACK
        )
        val line = WinChecker.checkWin(board, Point(7, 6))
        assertNull(line)
    }

    @Test
    fun `win detected from a corner move`() {
        val board = boardWith(
            Point(14, 14) to Stone.WHITE,
            Point(13, 13) to Stone.WHITE,
            Point(12, 12) to Stone.WHITE,
            Point(11, 11) to Stone.WHITE,
            Point(10, 10) to Stone.WHITE
        )
        val line = WinChecker.checkWin(board, Point(14, 14))
        assertNotNull(line)
        assertEquals(5, line!!.size)
    }
}
