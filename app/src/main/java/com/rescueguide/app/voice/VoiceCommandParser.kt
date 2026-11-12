package com.rescueguide.app.voice

/** Voice intents are navigation/control signals only; they never trigger clinical actions. */
enum class VoiceIntent { CONFIRM, DECLINE, REPEAT, BACK, UNKNOWN }

object PersianVoiceCommandParser {
    fun parse(raw: String): VoiceIntent {
        val value = normalize(raw)
        if (value.isBlank()) return VoiceIntent.UNKNOWN

        return when {
            value in confirmPhrases -> VoiceIntent.CONFIRM
            value in declinePhrases -> VoiceIntent.DECLINE
            value in repeatPhrases -> VoiceIntent.REPEAT
            value in backPhrases -> VoiceIntent.BACK
            else -> VoiceIntent.UNKNOWN
        }
    }

    internal fun normalize(raw: String): String = raw
        .lowercase()
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace('\u200c', ' ')
        .replace(Regex("[\\p{Punct}،؛؟!؟\\u0640]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private val confirmPhrases = setOf("بله", "آره", "اره", "تأیید", "تایید", "اوکی", "ok", "okay")
    private val declinePhrases = setOf("خیر", "نه", "نخیر", "no")
    private val repeatPhrases = setOf("تکرار", "دوباره", "دستور قبلی", "راهنمای قبلی", "مجدد بگو")
    private val backPhrases = setOf("بازگشت", "برگرد", "مرحله قبل", "قبلی", "عقب")
}
