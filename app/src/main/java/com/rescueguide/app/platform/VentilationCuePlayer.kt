package com.rescueguide.app.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Short, soft airflow cue to mark each rescue breath; it is an audio prompt, not a ventilator. */
class VentilationCuePlayer {
    private var track: AudioTrack? = null

    fun playBreathCue(): Boolean {
        stop()
        val sampleRate = 22_050
        val durationMs = 900
        val count = sampleRate * durationMs / 1000
        val data = ShortArray(count)
        val random = Random(731)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val envelope = when {
                t < 0.12 -> t / 0.12
                t > 0.78 -> ((0.90 - t) / 0.12).coerceAtLeast(0.0)
                else -> 1.0
            }
            val noise = (random.nextDouble() * 2.0 - 1.0) * 0.55
            val softAir = sin(2.0 * PI * 180.0 * t) * 0.18 + sin(2.0 * PI * 260.0 * t) * 0.10
            data[i] = ((noise + softAir) * envelope * 3_800).toInt().coerceIn(-7_000, 7_000).toShort()
        }
        return runCatching {
            val min = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val audio = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(maxOf(min, data.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            audio.write(data, 0, data.size)
            track = audio
            audio.play()
            true
        }.getOrDefault(false)
    }

    fun stop() {
        track?.runCatching { pause(); flush(); stop(); release() }
        track = null
    }
}
