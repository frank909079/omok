package com.frank.omok.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiPiece(
    val startXFraction: Float,
    val fallDelay: Float,
    val horizontalDrift: Float,
    val rotationSpeed: Float,
    val color: Color,
    val pieceSize: Float
)

private val CONFETTI_COLORS = listOf(
    Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFFB8C00)
)

private const val PIECE_COUNT = 90
private const val DURATION_MILLIS = 1800

@Composable
fun ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier) {
    val pieces = remember(trigger) {
        List(PIECE_COUNT) {
            ConfettiPiece(
                startXFraction = Random.nextFloat(),
                fallDelay = Random.nextFloat() * 0.3f,
                horizontalDrift = (Random.nextFloat() - 0.5f) * 0.4f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                color = CONFETTI_COLORS[Random.nextInt(CONFETTI_COLORS.size)],
                pieceSize = 6f + Random.nextFloat() * 6f
            )
        }
    }
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = DURATION_MILLIS, easing = LinearEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width
        pieces.forEach { piece ->
            val t = ((progress.value - piece.fallDelay) / (1f - piece.fallDelay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val y = t * (h + 40f) - 20f
            val x = piece.startXFraction * w + piece.horizontalDrift * w * t
            val alpha = if (t > 0.8f) (1f - (t - 0.8f) / 0.2f) else 1f
            rotate(degrees = piece.rotationSpeed * t, pivot = Offset(x, y)) {
                drawRect(
                    color = piece.color.copy(alpha = alpha),
                    topLeft = Offset(x - piece.pieceSize / 2, y - piece.pieceSize),
                    size = Size(piece.pieceSize, piece.pieceSize * 1.6f)
                )
            }
        }
    }
}
