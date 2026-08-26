package com.frank.omok.game

object WinChecker {
    private val DIRECTIONS = listOf(
        1 to 0,
        0 to 1,
        1 to 1,
        1 to -1
    )

    fun checkWin(board: Board, lastMove: Point): List<Point>? {
        val stone = board.stoneAt(lastMove.row, lastMove.col)
        if (stone == Stone.EMPTY) return null

        for ((dr, dc) in DIRECTIONS) {
            val line = mutableListOf(lastMove)

            var r = lastMove.row + dr
            var c = lastMove.col + dc
            while (inBounds(r, c) && board.stoneAt(r, c) == stone) {
                line.add(Point(r, c))
                r += dr
                c += dc
            }

            r = lastMove.row - dr
            c = lastMove.col - dc
            while (inBounds(r, c) && board.stoneAt(r, c) == stone) {
                line.add(0, Point(r, c))
                r -= dr
                c -= dc
            }

            if (line.size >= 5) return line
        }
        return null
    }

    private fun inBounds(row: Int, col: Int) =
        row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE
}
