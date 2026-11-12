package com.rescueguide.app.voice

import com.rescueguide.app.domain.PatientGroup

/** Voice intents are navigation/control signals only; they never trigger clinical actions. */
enum class VoiceIntent { CONFIRM, DECLINE, REPEAT, BACK, ROSC, UNKNOWN }

object PersianVoiceCommandParser {
    fun parse(raw: String): VoiceIntent {
        val value = normalize(raw)
        if (value.isBlank()) return VoiceIntent.UNKNOWN

        return when {
            value in confirmPhrases -> VoiceIntent.CONFIRM
            value in declinePhrases -> VoiceIntent.DECLINE
            value in repeatPhrases -> VoiceIntent.REPEAT
            value in backPhrases -> VoiceIntent.BACK
            value in roscPhrases -> VoiceIntent.ROSC
            else -> VoiceIntent.UNKNOWN
        }
    }

    /** Explicit age words are accepted; ages around puberty are not guessed. */
    fun parsePatientGroup(raw: String): PatientGroup? {
        val value = normalize(raw)
        if (value.contains("شیرخوار") || value.contains("زیر یک سال") || value.contains("کمتر از یک سال")) return PatientGroup.INFANT
        if (value.contains("بزرگسال") || value == "بالغ" || value.contains("نشانه بلوغ")) return PatientGroup.ADULT
        if (value.contains("کودک") || value.contains("بچه") || value.contains("قبل از بلوغ")) return PatientGroup.CHILD
        val digits = value.map { ch ->
            when (ch) {
                '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
                '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'; else -> ch
            }
        }.joinToString("")
        val months = Regex("""(\d+)\s*(ماه|ماهگی)""").find(digits)?.groupValues?.get(1)?.toIntOrNull()
        if (months != null) return if (months < 12) PatientGroup.INFANT else PatientGroup.CHILD
        val years = Regex("""(\d+)\s*(سال|ساله|سالگی)""").find(digits)?.groupValues?.get(1)?.toIntOrNull()
        return when {
            years == null || years < 1 -> null
            years >= 18 -> PatientGroup.ADULT
            else -> null // Age alone does not establish puberty status; choose child/adult explicitly.
        }
    }

    internal fun normalize(raw: String): String = raw
        .lowercase()
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace('\u200c', ' ')
        .filterNot { ch -> ch in ".,;:!?،؛؟!()[]{}<>\"'،ـ" }
        .replace(Regex("""\s+"""), " ")
        .trim()

    private val confirmPhrases = setOf("بله", "آره", "اره", "تأیید", "تایید", "اوکی", "ok", "okay")
    private val declinePhrases = setOf("خیر", "نه", "نخیر", "no")
    private val repeatPhrases = setOf("تکرار", "دوباره", "دستور قبلی", "راهنمای قبلی", "مجدد بگو")
    private val backPhrases = setOf("بازگشت", "برگرد", "مرحله قبل", "قبلی", "عقب")
    private val roscPhrases = setOf("علائم حیات برگشت", "علائم حیاتی برگشت", "تنفس طبیعی برگشت", "فرد دوباره نفس می کشد", "پاسخ دهی و تنفس طبیعی برگشت", "نبض و تنفس برگشت")
}
