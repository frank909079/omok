package com.frank.omok.game

enum class WinReason { FIVE_IN_A_ROW, OVERLINE }

sealed class GameResult {
    data object Playing : GameResult()
    data class Win(val winner: Stone, val line: List<Point>, val reason: WinReason) : GameResult()
    data object Draw : GameResult()
}
