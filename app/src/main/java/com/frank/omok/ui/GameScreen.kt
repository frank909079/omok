package com.frank.omok.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frank.omok.GameViewModel
import com.frank.omok.R
import com.frank.omok.game.GameResult
import com.frank.omok.game.Stone
import com.frank.omok.game.WinReason

@Composable
fun GameScreen(modifier: Modifier = Modifier, viewModel: GameViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusCard(
                level = uiState.level,
                wins = uiState.wins,
                losses = uiState.losses,
                bestLevel = uiState.bestLevel
            )

            Spacer(Modifier.height(12.dp))

            TurnIndicator(turn = uiState.turn, thinking = uiState.aiThinking)

            Spacer(Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                BoardCanvas(
                    board = uiState.board,
                    onTap = { row, col -> viewModel.onPlayerTap(row, col) },
                    lastMove = uiState.board.moves.lastOrNull(),
                    winLine = (uiState.result as? GameResult.Win)?.line
                )
            }

            Spacer(Modifier.height(16.dp))

            // Undo ("무르기"): disabled while there's nothing to undo, and while
            // the AI is still thinking — see the comment on GameViewModel.undo().
            OutlinedButton(
                onClick = { viewModel.undo() },
                enabled = uiState.canUndo && !uiState.aiThinking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("↩️  무르기")
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetStats() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("↺  전적 초기화")
                }
                Text(if (uiState.soundEnabled) "🔊" else "🔇")
                Switch(
                    checked = uiState.soundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) }
                )
            }

            Spacer(Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.pet_mascot),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }

        ConfettiOverlay(trigger = uiState.celebrationTrigger, modifier = Modifier.fillMaxSize())
    }

    val result = uiState.result
    val levelUpChoices = uiState.levelUpChoices
    if (levelUpChoices != null) {
        AlertDialog(
            onDismissRequest = {},
            icon = { PulsingTrophy() },
            title = { Text("흑 승리!") },
            text = { Text("레벨을 몇 단계 올릴까요? (현재 Lv ${uiState.level})") },
            confirmButton = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    levelUpChoices.forEach { delta ->
                        Button(onClick = { viewModel.chooseLevelUp(delta) }) {
                            Text("+$delta")
                        }
                    }
                }
            }
        )
    } else if (result != GameResult.Playing) {
        val isPlayerWin = result is GameResult.Win && result.winner == Stone.BLACK
        val (emoji, message) = when (result) {
            is GameResult.Win -> when {
                result.reason == WinReason.OVERLINE -> "💥" to "장목(6목 이상)으로 패배했습니다."
                result.winner == Stone.BLACK -> "🏆" to "흑 승리!"
                else -> "😵" to "백 승리!"
            }
            GameResult.Draw -> "🤝" to "무승부"
            GameResult.Playing -> "" to ""
        }
        AlertDialog(
            onDismissRequest = {},
            icon = if (isPlayerWin) {
                { PulsingTrophy() }
            } else null,
            title = { Text(if (isPlayerWin) "게임 종료" else "$emoji  게임 종료") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { viewModel.resetGame() }) {
                    Text("다시하기")
                }
            }
        )
    }
}

@Composable
private fun PulsingTrophy() {
    val infiniteTransition = rememberInfiniteTransition(label = "trophyPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyScale"
    )
    Text(
        text = "🏆",
        fontSize = 48.sp,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    )
}

@Composable
private fun StatusCard(level: Int, wins: Int, losses: Int, bestLevel: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lv $level",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "${wins}승 ${losses}패",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "최고 Lv $bestLevel",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun TurnIndicator(turn: Stone, thinking: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (thinking) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("생각 중…", fontSize = 16.sp)
        } else {
            val dotColor = if (turn == Stone.BLACK) Color(0xFF1B1B1B) else Color(0xFFF5F5F5)
            val dotBorder = if (turn == Stone.BLACK) Color.Transparent else Color(0xFF9E9E9E)
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(1.dp, dotBorder, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (turn == Stone.BLACK) "흑 차례" else "백 차례",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
