package com.rescueguide.app.platform

import android.content.Context
import android.media.MediaPlayer
import java.io.File

/**
 * Offline audio playback for bundled Persian prompt recordings.
 *
 * Put approved recordings in app/src/main/assets/audio/fa/ using the stable
 * filenames in Prompt. This player never invokes Android TextToSpeech.
 */
class OfflinePersianAudioGuide(context: Context) {
    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null
    private var cachedAudioFile: File? = null

    fun play(prompt: Prompt, onFinished: (() -> Unit)? = null, onError: ((String) -> Unit)? = null): Boolean {
        stop()
        return try {
            val path = "audio/fa/${prompt.assetName}"
            // Copy through the app cache so playback also works when aapt compresses MP3 assets.
            val audioFile = File.createTempFile("rescueguide-", ".mp3", appContext.cacheDir)
            appContext.assets.open(path).use { input -> audioFile.outputStream().use(input::copyTo) }
            cachedAudioFile = audioFile
            val next = MediaPlayer()
            next.setDataSource(audioFile.absolutePath)
            next.setOnCompletionListener {
                it.release()
                if (player === it) player = null
                cachedAudioFile?.delete()
                cachedAudioFile = null
                onFinished?.invoke()
            }
            next.setOnErrorListener { mediaPlayer, what, extra ->
                mediaPlayer.release()
                if (player === mediaPlayer) player = null
                cachedAudioFile?.delete()
                cachedAudioFile = null
                onError?.invoke("پخش فایل صوتی ناموفق بود (کد $what/$extra)")
                true
            }
            next.prepare()
            player = next
            next.start()
            true
        } catch (error: Exception) {
            player?.release()
            player = null
            cachedAudioFile?.delete()
            cachedAudioFile = null
            onError?.invoke("فایل صوتی ${prompt.assetName} هنوز در بسته برنامه موجود نیست.")
            false
        }
    }

    fun stop() {
        player?.runCatching { if (isPlaying) stop(); release() }
        player = null
        cachedAudioFile?.delete()
        cachedAudioFile = null
    }

    fun release() = stop()

    enum class Prompt(val assetName: String) {
        WELCOME("welcome.mp3"),
        CALL_EMERGENCY("call_115.mp3"),
        CHECK_RESPONSIVENESS("check_responsiveness.mp3"),
        START_COMPRESSIONS("start_compressions.mp3"),
        GIVE_BREATHS("give_breaths.mp3"),
        REPEAT_INSTRUCTION("repeat_instruction.mp3"),
        NORMAL_BREATHING("normal_breathing.mp3")
    }
}
