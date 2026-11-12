package com.rescueguide.app.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File
import java.security.MessageDigest

/** Plays bundled Persian MP3 prompts without network access or device TTS support. */
class OfflinePersianAudioPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null

    fun play(text: String): Boolean {
        stop()
        if (text.isBlank()) return false
        val assetName = sha256(text) + ".mp3"
        val cached = cachedAsset(assetName) ?: return false
        return runCatching {
            val next = MediaPlayer()
            player = next
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            next.setDataSource(cached.absolutePath)
            next.setOnCompletionListener { media ->
                media.release()
                if (player === media) player = null
            }
            next.setOnErrorListener { media, _, _ ->
                media.release()
                if (player === media) player = null
                true
            }
            next.prepare()
            next.start()
            true
        }.getOrElse {
            stop()
            false
        }
    }

    private fun cachedAsset(name: String): File? = runCatching {
        val directory = File(appContext.cacheDir, "offline_fa_audio").apply { mkdirs() }
        val target = File(directory, name)
        if (!target.exists() || target.length() == 0L) {
            appContext.assets.open("audio/$name").use { input -> target.outputStream().use(input::copyTo) }
        }
        target.takeIf { it.exists() && it.length() > 0L }
    }.getOrNull()

    fun stop() {
        player?.let { current -> runCatching { current.stop(); current.release() } }
        player = null
    }

    fun release() = stop()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
