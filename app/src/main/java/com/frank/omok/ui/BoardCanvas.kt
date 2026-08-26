package com.frank.omok.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.frank.omok.game.BOARD_SIZE
import com.frank.omok.game.Board
import com.frank.omok.game.Point
import com.frank.omok.game.Stone
import kotlin.math.roundToInt

private val STAR_POINTS = listOf(3 to 3, 3 to 11, 11 to 3, 11 to 11, 7 to 7)
private val BOARD_BACKGROUND = Color(0xFFDDB88C)
private val LINE_COLOR = Color(0xFF5A3B22)
private val BLACK_STONE = Color(0xFF1B1B1B)
private val BLACK_STONE_RIM = Color(0xFF5A5A5A)
private val WHITE_STONE = Color(0xFFF5F5F5)
private val WHITE_STONE_RIM = Color(0xFF9E9E9E)
private val LAST_MOVE_MARKER = Color(0xFFE53935)
private val WIN_LINE_COLOR = Color(0xFFE53935)

@Composable
fun BoardCanvas(
    board: Board,
    onTap: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
    lastMove: Point? = null,
    winLine: List<Point>? = null
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cell = size.width.toFloat() / BOARD_SIZE
                    val half = cell / 2f
                    val col = ((offset.x - half) / cell).roundToInt().coerceIn(0, BOARD_SIZE - 1)
                    val row = ((offset.y - half) / cell).roundToInt().coerceIn(0, BOARD_SIZE - 1)
                    onTap(row, col)
                }
            }
    ) {
        val cell = size.width / BOARD_SIZE
        val half = cell / 2f
        val lastLine = half + (BOARD_SIZE - 1) * cell
        val stoneRadius = cell * 0.42f

        drawRect(color = BOARD_BACKGROUND, size = size)

        for (i in 0 until BOARD_SIZE) {
            val pos = half + i * cell
            drawLine(LINE_COLOR, Offset(half, pos), Offset(lastLine, pos), strokeWidth = 2f)
            drawLine(LINE_COLOR, Offset(pos, half), Offset(pos, lastLine), strokeWidth = 2f)
        }

        STAR_POINTS.forEach { (r, c) ->
            drawCircle(LINE_COLOR, radius = cell * 0.08f, center = Offset(half + c * cell, half + r * cell))
        }

        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val stone = board.stoneAt(row, col)
                if (stone != Stone.EMPTY) {
                    val center = Offset(half + col * cell, half + row * cell)
                    val fill = if (stone == Stone.BLACK) BLACK_STONE else WHITE_STONE
                    val rim = if (stone == Stone.BLACK) BLACK_STONE_RIM else WHITE_STONE_RIM
                    drawCircle(color = fill, radius = stoneRadius, center = center)
                    drawCircle(color = rim, radius = stoneRadius, center = center, style = Stroke(width = 1.5f))
                }
            }
        }

        if (winLine != null && winLine.size >= 2) {
            val start = Offset(half + winLine.first().col * cell, half + winLine.first().row * cell)
            val end = Offset(half + winLine.last().col * cell, half + winLine.last().row * cell)
            drawLine(
                color = WIN_LINE_COLOR.copy(alpha = 0.85f),
                start = start,
                end = end,
                strokeWidth = cell * 0.12f
            )
        }

        lastMove?.let { last ->
            val center = Offset(half + last.col * cell, half + last.row * cell)
            drawCircle(color = LAST_MOVE_MARKER, radius = stoneRadius * 0.22f, center = center)
        }
    }
}
