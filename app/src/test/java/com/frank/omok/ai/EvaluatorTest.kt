package com.frank.omok.ai

import com.frank.omok.game.Board
import com.frank.omok.game.Point
import com.frank.omok.game.Stone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluatorTest {

    private fun boardWith(vararg points: Pair<Point, Stone>): Board {
        var board = Board()
        for ((point, stone) in points) {
            board = board.place(point.row, point.col, stone)
        }
        return board
    }

    @Test
    fun `open four scores higher than closed four`() {
        val openFourBoard = boardWith(
            Point(7, 5) to Stone.BLACK,
            Point(7, 6) to Stone.BLACK,
            Point(7, 7) to Stone.BLACK
        )
        val openFourScore = Evaluator.scorePoint(openFourBoard, Point(7, 8), Stone.BLACK)

        val closedFourBoard = boardWith(
            Point(7, 5) to Stone.BLACK,
            Point(7, 6) to Stone.BLACK,
            Point(7, 7) to Stone.BLACK,
            Point(7, 4) to Stone.WHITE
        )
        val closedFourScore = Evaluator.scorePoint(closedFourBoard, Point(7, 8), Stone.BLACK)

        assertTrue(openFourScore > closedFourScore)
    }

    @Test
    fun `five in a row scores highest of all`() {
        val board = boardWith(
            Point(3, 3) to Stone.WHITE,
            Point(3, 4) to Stone.WHITE,
            Point(3, 5) to Stone.WHITE,
            Point(3, 6) to Stone.WHITE
        )
        val fiveScore = Evaluator.scorePoint(board, Point(3, 7), Stone.WHITE)
        assertEquals(Evaluator.FIVE, fiveScore)
    }

    @Test
    fun `open three scores higher than open two`() {
        val openThreeBoard = boardWith(
            Point(5, 5) to Stone.BLACK,
            Point(5, 6) to Stone.BLACK
        )
        val openThreeScore = Evaluator.scorePoint(openThreeBoard, Point(5, 7), Stone.BLACK)

        val openTwoBoard = boardWith(
            Point(9, 5) to Stone.BLACK
        )
        val openTwoScore = Evaluator.scorePoint(openTwoBoard, Point(9, 6), Stone.BLACK)

        assertTrue(openThreeScore > openTwoScore)
    }
}
