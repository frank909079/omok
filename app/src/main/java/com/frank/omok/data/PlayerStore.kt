package com.frank.omok.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "player_stats")

data class PlayerStats(
    val level: Int = PlayerStore.MIN_LEVEL,
    val wins: Int = 0,
    val losses: Int = 0,
    val bestLevel: Int = PlayerStore.MIN_LEVEL,
    val soundEnabled: Boolean = true
)

class PlayerStore(private val context: Context) {

    private object Keys {
        val LEVEL = intPreferencesKey("level")
        val WINS = intPreferencesKey("wins")
        val LOSSES = intPreferencesKey("losses")
        val BEST_LEVEL = intPreferencesKey("best_level")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    }

    val stats: Flow<PlayerStats> = context.dataStore.data.map { prefs ->
        PlayerStats(
            level = prefs[Keys.LEVEL] ?: MIN_LEVEL,
            wins = prefs[Keys.WINS] ?: 0,
            losses = prefs[Keys.LOSSES] ?: 0,
            bestLevel = prefs[Keys.BEST_LEVEL] ?: MIN_LEVEL,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true
        )
    }

    suspend fun recordWin(levelDelta: Int = 1) {
        context.dataStore.edit { prefs ->
            val newLevel = ((prefs[Keys.LEVEL] ?: MIN_LEVEL) + levelDelta).coerceAtMost(MAX_LEVEL)
            prefs[Keys.LEVEL] = newLevel
            prefs[Keys.WINS] = (prefs[Keys.WINS] ?: 0) + 1
            prefs[Keys.BEST_LEVEL] = maxOf(prefs[Keys.BEST_LEVEL] ?: MIN_LEVEL, newLevel)
        }
    }

    suspend fun recordLoss() {
        context.dataStore.edit { prefs ->
            prefs[Keys.LEVEL] = ((prefs[Keys.LEVEL] ?: MIN_LEVEL) - 1).coerceAtLeast(MIN_LEVEL)
            prefs[Keys.LOSSES] = (prefs[Keys.LOSSES] ?: 0) + 1
        }
    }

    suspend fun resetStats() {
        context.dataStore.edit { prefs ->
            prefs[Keys.LEVEL] = MIN_LEVEL
            prefs[Keys.WINS] = 0
            prefs[Keys.LOSSES] = 0
            prefs[Keys.BEST_LEVEL] = MIN_LEVEL
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = enabled
        }
    }

    companion object {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 100
    }
}
