package com.frank.omok.ai

import com.frank.omok.game.Board
import com.frank.omok.game.Point
import com.frank.omok.game.Stone
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTest {

    private fun boardWith(vararg points: Pair<Point, Stone>): Board {
        var board = Board()
        for ((point, stone) in points) {
            board = board.place(point.row, point.col, stone)
        }
        return board
    }

    @Test
    fun `AI blocks an open three immediately instead of waiting for an open four`() {
        val board = boardWith(
            Point(7, 5) to Stone.BLACK,
            Point(7, 6) to Stone.BLACK,
            Point(7, 7) to Stone.BLACK
        )
        val move = Ai.chooseMoveForLevel(board, myStone = Stone.WHITE, opponentStone = Stone.BLACK, level = 30)
        val blocksThreat = move == Point(7, 4) || move == Point(7, 8)
        assertTrue("expected AI to block the open three at (7,4) or (7,8), but played $move", blocksThreat)
    }
}
