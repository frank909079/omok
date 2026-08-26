package com.frank.omok

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frank.omok.ai.Ai
import com.frank.omok.ai.LevelConfig
import com.frank.omok.data.PlayerStore
import com.frank.omok.game.Board
import com.frank.omok.game.GameResult
import com.frank.omok.game.GameRules
import com.frank.omok.game.Point
import com.frank.omok.game.Stone
import com.frank.omok.ui.Feedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameUiState(
    val board: Board = Board(),
    val turn: Stone = Stone.BLACK,
    val result: GameResult = GameResult.Playing,
    val aiThinking: Boolean = false,
    val level: Int = PlayerStore.MIN_LEVEL,
    val wins: Int = 0,
    val losses: Int = 0,
    val bestLevel: Int = PlayerStore.MIN_LEVEL,
    val levelUpChoices: List<Int>? = null,
    val soundEnabled: Boolean = true,
    val celebrationTrigger: Int = 0,
    // Whether the Undo button should be tappable right now — true only when
    // there is at least one saved board to go back to (see `undoHistory` below).
    val canUndo: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val playerStone = Stone.BLACK
    private val aiStone = Stone.WHITE
    private val playerStore = PlayerStore(application)
    private val feedback = Feedback(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Undo history: a stack (LIFO — Last In, First Out) of past board states.
    // Board is immutable (place() always returns a *new* Board, see Board.kt),
    // so every past board is still a valid, untouched snapshot we can just hand
    // back later — no need to "erase" a stone, we just swap the whole board out.
    // We push onto this right before the player's move is applied, so popping
    // one entry rewinds past both that move AND the AI's reply to it.
    private val undoHistory = ArrayDeque<Board>()

    init {
        viewModelScope.launch {
            playerStore.stats.collect { stats ->
                feedback.soundEnabled = stats.soundEnabled
                _uiState.update {
                    it.copy(
                        level = stats.level,
                        wins = stats.wins,
                        losses = stats.losses,
                        bestLevel = stats.bestLevel,
                        soundEnabled = stats.soundEnabled
                    )
                }
            }
        }
    }

    fun onPlayerTap(row: Int, col: Int) {
        val state = _uiState.value
        if (state.result != GameResult.Playing) return
        if (state.turn != playerStone) return
        if (!state.board.isEmpty(row, col)) {
            Log.d("Omok", "occupied -> $row,$col ignored")
            return
        }

        // Save the board exactly as it is NOW, before this move is applied.
        // This is the snapshot Undo will restore later.
        undoHistory.addLast(state.board)

        val newBoard = state.board.place(row, col, playerStone)
        applyMove(newBoard, Point(row, col), playerStone)

        if (_uiState.value.result == GameResult.Playing) {
            requestAiMove()
        }
    }

    private fun requestAiMove() {
        _uiState.update { it.copy(aiThinking = true) }
        viewModelScope.launch {
            val board = _uiState.value.board
            val level = _uiState.value.level
            val startTime = System.currentTimeMillis()
            val move = withContext(Dispatchers.Default) {
                val config = LevelConfig.configForLevel(level)
                val ranked = Ai.rankMoves(board, aiStone, playerStone, config.defenseWeight)
                Log.d("Omok", "Lv$level config=$config top candidates: ${ranked.take(5)}")
                Ai.chooseMoveForLevel(board, aiStone, playerStone, level)
            }
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < MIN_THINK_MILLIS) {
                delay(MIN_THINK_MILLIS - elapsed)
            }
            val newBoard = board.place(move.row, move.col, aiStone)
            applyMove(newBoard, move, aiStone)
            _uiState.update { it.copy(aiThinking = false) }
        }
    }

    private fun applyMove(newBoard: Board, point: Point, stone: Stone) {
        val result = GameRules.evaluateMove(newBoard, point, stone)
        feedback.onStonePlaced()
        val nextTurn = if (stone == Stone.BLACK) Stone.WHITE else Stone.BLACK
        _uiState.update {
            it.copy(
                board = newBoard,
                turn = if (result == GameResult.Playing) nextTurn else it.turn,
                result = result,
                canUndo = undoHistory.isNotEmpty()
            )
        }

        if (result is GameResult.Win) {
            if (result.winner == playerStone) {
                feedback.onWin()
                _uiState.update { it.copy(celebrationTrigger = it.celebrationTrigger + 1) }
                val currentLevel = _uiState.value.level
                if (currentLevel < LEVEL_UP_PICKER_CAP) {
                    val maxDelta = (LEVEL_UP_PICKER_CAP - currentLevel).coerceAtMost(MAX_LEVEL_UP_STEP)
                    _uiState.update { it.copy(levelUpChoices = (1..maxDelta).toList()) }
                } else {
                    viewModelScope.launch { playerStore.recordWin() }
                }
            } else {
                feedback.onLose()
                viewModelScope.launch { playerStore.recordLoss() }
            }
        } else if (result is GameResult.Draw) {
            feedback.onDraw()
        }
    }

    // Pops one snapshot off the undo stack and makes it the current board again.
    // Popping rewinds past the player's last move AND the AI's reply to it, so
    // the turn always comes back to the player. Calling this repeatedly walks
    // further back in history, one full turn at a time.
    fun undo() {
        val state = _uiState.value
        // Don't rewind mid-AI-turn: the AI's move is still being computed on a
        // background thread, and letting it land after we've already jumped to
        // an older board would silently resurrect a stone we just undid.
        if (state.aiThinking) return

        val previousBoard = undoHistory.removeLastOrNull() ?: return
        _uiState.update {
            it.copy(
                board = previousBoard,
                turn = playerStone,
                result = GameResult.Playing,
                levelUpChoices = null,
                canUndo = undoHistory.isNotEmpty()
            )
        }
    }

    fun chooseLevelUp(delta: Int) {
        viewModelScope.launch { playerStore.recordWin(delta) }
        undoHistory.clear()
        _uiState.update {
            it.copy(
                board = Board(),
                turn = Stone.BLACK,
                result = GameResult.Playing,
                aiThinking = false,
                levelUpChoices = null,
                canUndo = false
            )
        }
    }

    fun resetGame() {
        undoHistory.clear()
        _uiState.update {
            it.copy(
                board = Board(),
                turn = Stone.BLACK,
                result = GameResult.Playing,
                aiThinking = false,
                levelUpChoices = null,
                canUndo = false
            )
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { playerStore.setSoundEnabled(enabled) }
    }

    fun resetStats() {
        viewModelScope.launch { playerStore.resetStats() }
    }

    override fun onCleared() {
        super.onCleared()
        feedback.release()
    }

    companion object {
        private const val LEVEL_UP_PICKER_CAP = 30
        private const val MAX_LEVEL_UP_STEP = 5
        private const val MIN_THINK_MILLIS = 300L
    }
}
