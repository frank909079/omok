package com.frank.omok.ai

import com.frank.omok.game.BOARD_SIZE
import com.frank.omok.game.Board
import com.frank.omok.game.Point
import com.frank.omok.game.Stone

object Evaluator {
    private val DIRECTIONS = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)

    const val FIVE = 1_000_000
    const val OPEN_FOUR = 100_000
    const val CLOSED_FOUR = 10_000
    const val OPEN_THREE = 5_000
    const val CLOSED_THREE = 500
    const val OPEN_TWO = 100

    fun scorePoint(board: Board, point: Point, stone: Stone): Int {
        val simulated = board.place(point.row, point.col, stone)
        var total = 0
        for ((dr, dc) in DIRECTIONS) {
            total += scoreDirection(simulated, point, stone, dr, dc)
        }
        return total
    }

    private fun scoreDirection(board: Board, point: Point, stone: Stone, dr: Int, dc: Int): Int {
        var count = 1

        var r = point.row + dr
        var c = point.col + dc
        while (inBounds(r, c) && board.stoneAt(r, c) == stone) {
            count++
            r += dr
            c += dc
        }
        val openForward = inBounds(r, c) && board.stoneAt(r, c) == Stone.EMPTY

        r = point.row - dr
        c = point.col - dc
        while (inBounds(r, c) && board.stoneAt(r, c) == stone) {
            count++
            r -= dr
            c -= dc
        }
        val openBackward = inBounds(r, c) && board.stoneAt(r, c) == Stone.EMPTY

        val openEnds = (if (openForward) 1 else 0) + (if (openBackward) 1 else 0)

        return when {
            count >= 5 -> FIVE
            count == 4 -> if (openEnds == 2) OPEN_FOUR else if (openEnds == 1) CLOSED_FOUR else 0
            count == 3 -> if (openEnds == 2) OPEN_THREE else if (openEnds == 1) CLOSED_THREE else 0
            count == 2 -> if (openEnds == 2) OPEN_TWO else 0
            else -> 0
        }
    }

    private fun inBounds(row: Int, col: Int) = row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE
}
