package com.frank.omok.ai

import com.frank.omok.game.BOARD_SIZE
import com.frank.omok.game.Board
import com.frank.omok.game.GameResult
import com.frank.omok.game.GameRules
import com.frank.omok.game.Point
import com.frank.omok.game.Stone

object Search {
    private const val INF = 100_000_000
    private const val WIN_VALUE = 10_000_000
    private val DIRECTIONS = listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)

    fun findBestMove(
        board: Board,
        myStone: Stone,
        opponentStone: Stone,
        depth: Int,
        defenseWeight: Double,
        deadlineMillis: Long
    ): Point {
        val width = candidateWidthForDepth(depth)
        val candidates = Ai.rankMoves(board, myStone, opponentStone, defenseWeight).take(width)
        if (candidates.isEmpty()) return Ai.chooseMove(board, myStone, opponentStone, defenseWeight)

        var bestPoint = candidates.first().point
        var bestValue = -INF
        var alpha = -INF
        val beta = INF

        for (candidate in candidates) {
            if (System.currentTimeMillis() > deadlineMillis) break
            val nextBoard = board.place(candidate.point.row, candidate.point.col, myStone)
            val value = terminalValueOrNull(nextBoard, candidate.point, myStone) ?: -negamax(
                board = nextBoard,
                toMove = opponentStone,
                otherSide = myStone,
                depth = depth - 1,
                defenseWeight = defenseWeight,
                alpha = -beta,
                beta = -alpha,
                deadlineMillis = deadlineMillis,
                width = width
            )
            if (value > bestValue) {
                bestValue = value
                bestPoint = candidate.point
            }
            alpha = maxOf(alpha, value)
        }
        return bestPoint
    }

    private fun negamax(
        board: Board,
        toMove: Stone,
        otherSide: Stone,
        depth: Int,
        defenseWeight: Double,
        alpha: Int,
        beta: Int,
        deadlineMillis: Long,
        width: Int
    ): Int {
        val candidates = Ai.rankMoves(board, toMove, otherSide, defenseWeight).take(width)
        if (candidates.isEmpty()) return 0
        if (depth <= 0 || System.currentTimeMillis() > deadlineMillis) {
            return candidates.first().score
        }

        var value = -INF
        var a = alpha
        for (candidate in candidates) {
            if (System.currentTimeMillis() > deadlineMillis) break
            val nextBoard = board.place(candidate.point.row, candidate.point.col, toMove)
            val childValue = terminalValueOrNull(nextBoard, candidate.point, toMove) ?: -negamax(
                board = nextBoard,
                toMove = otherSide,
                otherSide = toMove,
                depth = depth - 1,
                defenseWeight = defenseWeight,
                alpha = -beta,
                beta = -a,
                deadlineMillis = deadlineMillis,
                width = width
            )
            value = maxOf(value, childValue)
            a = maxOf(a, value)
            if (a >= beta) break
        }
        return value
    }

    /**
     * Non-null if [mover]'s move at [point] already decides the game: +WIN_VALUE if [mover] won
     * outright, or set up an open four (unstoppable next turn, since blocking one end still
     * leaves the other), -WIN_VALUE if it was an overline self-loss. An open-four check is needed
     * because the leaf heuristic only looks at the opponent's single best reply, and a single
     * move can't block both ends of an open four - without this, the search sees the opponent's
     * big-looking blocking move and wrongly concludes the position is fine.
     */
    private fun terminalValueOrNull(board: Board, point: Point, mover: Stone): Int? {
        val result = GameRules.evaluateMove(board, point, mover)
        if (result is GameResult.Win) {
            return if (result.winner == mover) WIN_VALUE else -WIN_VALUE
        }
        return if (hasUnstoppableOpenFour(board, point, mover)) WIN_VALUE else null
    }

    private fun hasUnstoppableOpenFour(board: Board, point: Point, mover: Stone): Boolean {
        for ((dr, dc) in DIRECTIONS) {
            val ends = openFourEnds(board, point, mover, dr, dc) ?: continue
            for (end in ends) {
                val completed = board.place(end.row, end.col, mover)
                val result = GameRules.evaluateMove(completed, end, mover)
                if (result is GameResult.Win && result.winner == mover) return true
            }
        }
        return false
    }

    /** If the run of [stone] through [point] along ([dr],[dc]) is exactly 4 with both ends open, returns those two ends. */
    private fun openFourEnds(board: Board, point: Point, stone: Stone, dr: Int, dc: Int): List<Point>? {
        var count = 1

        var r = point.row + dr
        var c = point.col + dc
        while (inBounds(r, c) && board.stoneAt(r, c) == stone) {
            count++
            r += dr
            c += dc
        }
        val forwardEnd = Point(r, c)
        val openForward = inBounds(r, c) && board.stoneAt(r, c) == Stone.EMPTY

        r = point.row - dr
        c = point.col - dc
        while (inBounds(r, c) && board.stoneAt(r, c) == stone) {
            count++
            r -= dr
            c -= dc
        }
        val backwardEnd = Point(r, c)
        val openBackward = inBounds(r, c) && board.stoneAt(r, c) == Stone.EMPTY

        return if (count == 4 && openForward && openBackward) listOf(forwardEnd, backwardEnd) else null
    }

    private fun inBounds(row: Int, col: Int) = row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE

    private fun candidateWidthForDepth(depth: Int): Int = when {
        depth <= 2 -> 10
        depth <= 4 -> 8
        else -> 6
    }
}
