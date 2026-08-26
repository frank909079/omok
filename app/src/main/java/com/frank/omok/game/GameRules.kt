package com.frank.omok.game

object GameRules {
    fun evaluateMove(board: Board, lastMove: Point, mover: Stone): GameResult {
        val line = WinChecker.checkWin(board, lastMove)
        return when {
            line != null && mover == Stone.BLACK && line.size > 5 ->
                GameResult.Win(winner = opponentOf(mover), line = line, reason = WinReason.OVERLINE)
            line != null ->
                GameResult.Win(winner = mover, line = line, reason = WinReason.FIVE_IN_A_ROW)
            board.moves.size == BOARD_SIZE * BOARD_SIZE ->
                GameResult.Draw
            else -> GameResult.Playing
        }
    }

    private fun opponentOf(stone: Stone) = if (stone == Stone.BLACK) Stone.WHITE else Stone.BLACK
}
