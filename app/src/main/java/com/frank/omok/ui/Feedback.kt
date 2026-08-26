package com.frank.omok.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.frank.omok.R
import kotlin.random.Random

class Feedback(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var clickSoundId = 0
    private var clickSoundLoaded = false
    private var winSoundId = 0
    private var winSoundLoaded = false

    init {
        clickSoundId = soundPool.load(appContext, R.raw.stone_click, 1)
        winSoundId = soundPool.load(appContext, R.raw.win_fanfare, 1)
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) return@setOnLoadCompleteListener
            if (sampleId == clickSoundId) clickSoundLoaded = true
            if (sampleId == winSoundId) winSoundLoaded = true
        }
    }

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // In-app sound toggle, set by GameViewModel from the persisted preference. This is
    // independent of media-volume muting (isMuted below) - both must allow sound to play.
    var soundEnabled: Boolean = true

    // Checked against media volume, not ringer mode: ringer silent/vibrate only affects
    // ringtones and notifications on Android, not the media/game stream SoundPool uses
    // here. Gating on ringer mode would wrongly silence sound any time the phone is in
    // vibrate mode, which is a very common everyday state, not an explicit mute request.
    private val isMuted: Boolean
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0

    private val shouldPlaySound: Boolean
        get() = soundEnabled && !isMuted

    fun onStonePlaced() {
        if (shouldPlaySound && clickSoundLoaded) {
            val rate = MIN_PITCH_RATE + Random.nextFloat() * (MAX_PITCH_RATE - MIN_PITCH_RATE)
            soundPool.play(clickSoundId, 1f, 1f, 1, 0, rate)
        }
        vibrate(STONE_VIBRATION_MS)
    }

    fun onWin() {
        if (shouldPlaySound && winSoundLoaded) {
            soundPool.play(winSoundId, 1f, 1f, 1, 0, 1f)
        }
        vibrator.vibrate(VibrationEffect.createWaveform(WIN_VIBRATION_PATTERN, -1))
    }

    fun onLose() {
        if (shouldPlaySound) toneGenerator.startTone(ToneGenerator.TONE_CDMA_REORDER, 300)
        vibrate(RESULT_VIBRATION_MS)
    }

    fun onDraw() {
        if (shouldPlaySound) toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        vibrate(DRAW_VIBRATION_MS)
    }

    private fun vibrate(durationMillis: Long) {
        vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun release() {
        toneGenerator.release()
        soundPool.release()
    }

    companion object {
        private const val STONE_VIBRATION_MS = 33L
        private const val RESULT_VIBRATION_MS = 150L
        private const val DRAW_VIBRATION_MS = 80L
        private const val MIN_PITCH_RATE = 0.95f
        private const val MAX_PITCH_RATE = 1.05f

        // Short double-tap building into a longer buzz, timed roughly with the fanfare's rhythm.
        private val WIN_VIBRATION_PATTERN = longArrayOf(0, 60, 40, 60, 40, 200)
    }
}
