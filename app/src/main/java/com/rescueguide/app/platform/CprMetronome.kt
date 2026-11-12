package com.rescueguide.app.platform

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Independent compression-rate metronome; it does not measure compression depth or quality. */
class CprMetronome(private val scope: CoroutineScope) {
    private var tone: ToneGenerator? = null
    private var job: Job? = null
    var isRunning: Boolean = false
        private set

    fun start(beatsPerMinute: Int): Boolean {
        stop()
        val bpm = beatsPerMinute.coerceIn(100, 120)
        val generator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 75) }.getOrNull() ?: return false
        tone = generator
        job = scope.launch {
            isRunning = true
            val interval = 60_000L / bpm
            while (isActive) {
                generator.startTone(ToneGenerator.TONE_PROP_BEEP, 85)
                delay(interval)
            }
        }
        return true
    }

    fun stop() {
        job?.cancel()
        job = null
        isRunning = false
        tone?.runCatching { release() }
        tone = null
    }

    fun release() = stop()
}
