package com.frank.omok.ai

import com.frank.omok.game.BOARD_SIZE
import com.frank.omok.game.Board
import com.frank.omok.game.Point
import com.frank.omok.game.Stone
import kotlin.random.Random

data class ScoredMove(val point: Point, val score: Int)

object Ai {
    private const val NEIGHBOR_RADIUS = 2
    private const val BLUNDER_POOL_SIZE = 5
    private const val DEFAULT_TIME_BUDGET_MILLIS = 2000L
    private const val URGENT_DEFENSE_WEIGHT = 1.0
    const val DEFAULT_DEFENSE_WEIGHT = 1.1

    fun rankMoves(
        board: Board,
        myStone: Stone,
        opponentStone: Stone,
        defenseWeight: Double = DEFAULT_DEFENSE_WEIGHT
    ): List<ScoredMove> {
        return candidateMoves(board).map { point ->
            val myGain = Evaluator.scorePoint(board, point, myStone)
            val opponentGain = Evaluator.scorePoint(board, point, opponentStone)
            // An open three (or bigger) becomes an unstoppable open four next turn if
            // ignored, so it's always worth blocking regardless of the level's usual
            // defenseWeight discount. blunderRate is the only sanctioned way a weak
            // level still misses it.
            val effectiveWeight = if (opponentGain >= Evaluator.OPEN_THREE) {
                maxOf(defenseWeight, URGENT_DEFENSE_WEIGHT)
            } else {
                defenseWeight
            }
            val score = myGain + (effectiveWeight * opponentGain).toInt()
            ScoredMove(point, score)
        }.sortedByDescending { it.score }
    }

    fun chooseMove(
        board: Board,
        myStone: Stone,
        opponentStone: Stone,
        defenseWeight: Double = DEFAULT_DEFENSE_WEIGHT
    ): Point = rankMoves(board, myStone, opponentStone, defenseWeight).first().point

    fun chooseMoveForLevel(
        board: Board,
        myStone: Stone,
        opponentStone: Stone,
        level: Int,
        timeBudgetMillis: Long = DEFAULT_TIME_BUDGET_MILLIS
    ): Point {
        val config = LevelConfig.configForLevel(level)
        val ranked = rankMoves(board, myStone, opponentStone, config.defenseWeight)

        if (Random.nextDouble() < config.blunderRate) {
            return pickBlunder(ranked)
        }

        return if (config.depth <= 0) {
            ranked.first().point
        } else {
            val deadline = System.currentTimeMillis() + timeBudgetMillis
            Search.findBestMove(board, myStone, opponentStone, config.depth, config.defenseWeight, deadline)
        }
    }

    private fun pickBlunder(ranked: List<ScoredMove>): Point {
        val pool = ranked.take(BLUNDER_POOL_SIZE)
        var roll = Random.nextInt(pool.indices.sumOf { pool.size - it })
        for ((index, move) in pool.withIndex()) {
            val weight = pool.size - index
            if (roll < weight) return move.point
            roll -= weight
        }
        return pool.last().point
    }

    private fun candidateMoves(board: Board): List<Point> {
        if (board.moves.isEmpty()) {
            val center = BOARD_SIZE / 2
            return listOf(Point(center, center))
        }

        val candidates = LinkedHashSet<Point>()
        for (move in board.moves) {
            for (dr in -NEIGHBOR_RADIUS..NEIGHBOR_RADIUS) {
                for (dc in -NEIGHBOR_RADIUS..NEIGHBOR_RADIUS) {
                    val r = move.row + dr
                    val c = move.col + dc
                    if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE && board.isEmpty(r, c)) {
                        candidates.add(Point(r, c))
                    }
                }
            }
        }
        return candidates.toList()
    }
}
