package com.rescueguide.app.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/** TTS adapter reports device capability; it never assumes Persian voice data is installed. */
class AndroidPersianSpeechOutput(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var engine: TextToSpeech? = null
    private var focusRequest: AudioFocusRequest? = null
    private var focusHeld = false
    private var lastTestUtterance: String? = null
    private val utteranceIds = AtomicLong(0)

    var state: State = State.INITIALIZING
        private set
    private var onReady: ((State) -> Unit)? = null

    init { engine = TextToSpeech(appContext, this) }

    fun observeReady(callback: (State) -> Unit) {
        onReady = callback
        if (state != State.INITIALIZING) callback(state)
    }

    override fun onInit(status: Int) {
        val tts = engine
        state = if (status == TextToSpeech.SUCCESS && tts != null) {
            val availability = tts.isLanguageAvailable(Locale("fa", "IR"))
            when {
                availability == TextToSpeech.LANG_MISSING_DATA -> State.LANGUAGE_DATA_MISSING
                availability == TextToSpeech.LANG_NOT_SUPPORTED -> State.LANGUAGE_UNSUPPORTED
                else -> {
                    tts.language = Locale("fa", "IR")
                    installProgressListener(tts)
                    State.READY
                }
            }
        } else State.INIT_FAILED
        onReady?.invoke(state)
    }

    /** Test-only speech entry point; no clinical prompts are supplied by this prototype. */
    fun speakTestPhrase(text: String): Boolean {
        if (state != State.READY || text.isBlank() || !requestAudioFocus()) return false
        lastTestUtterance = text
        return enqueue(text)
    }

    /** Replays only the last explicitly supplied non-clinical hardware-test phrase. */
    fun repeatLastTestPhrase(): Boolean {
        val text = lastTestUtterance ?: return false
        if (state != State.READY || !requestAudioFocus()) return false
        return enqueue(text)
    }

    private fun enqueue(text: String): Boolean {
        val id = "rescueguide-test-${utteranceIds.incrementAndGet()}"
        val result = engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (result == TextToSpeech.ERROR) {
            abandonAudioFocus()
            return false
        }
        return result == TextToSpeech.SUCCESS
    }

    private fun installProgressListener(tts: TextToSpeech) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = abandonAudioFocus()
            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String?) = abandonAudioFocus()
            override fun onError(utteranceId: String?, errorCode: Int) = abandonAudioFocus()
        })
    }

    private fun requestAudioFocus(): Boolean {
        if (focusHeld) return true
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    // Do not auto-resume or replay over another app; the user can explicitly repeat.
                    engine?.stop()
                    abandonAudioFocus()
                }
            }
            .build()
        focusRequest = request
        focusHeld = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return focusHeld
    }

    private fun abandonAudioFocus() {
        val request = focusRequest ?: return
        if (focusHeld) audioManager.abandonAudioFocusRequest(request)
        focusHeld = false
        focusRequest = null
    }

    fun stop() { engine?.stop(); abandonAudioFocus() }
    fun shutdown() { stop(); engine?.shutdown(); engine = null; lastTestUtterance = null }

    enum class State { INITIALIZING, READY, LANGUAGE_DATA_MISSING, LANGUAGE_UNSUPPORTED, INIT_FAILED }
}
