package com.rescueguide.app.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandParserTest {
    @Test fun recognizesPersianAndEnglishConfirmations() {
        assertEquals(VoiceIntent.CONFIRM, PersianVoiceCommandParser.parse("بله"))
        assertEquals(VoiceIntent.CONFIRM, PersianVoiceCommandParser.parse("اوکی!"))
        assertEquals(VoiceIntent.CONFIRM, PersianVoiceCommandParser.parse("OK"))
    }

    @Test fun normalizesArabicCharactersAndZeroWidthNonJoiner() {
        assertEquals(VoiceIntent.CONFIRM, PersianVoiceCommandParser.parse("تأیید"))
        assertEquals(VoiceIntent.REPEAT, PersianVoiceCommandParser.parse("دستور‌قبلی"))
    }

    @Test fun recognizesDeclineRepeatAndBack() {
        assertEquals(VoiceIntent.DECLINE, PersianVoiceCommandParser.parse("خیر"))
        assertEquals(VoiceIntent.REPEAT, PersianVoiceCommandParser.parse("تکرار"))
        assertEquals(VoiceIntent.BACK, PersianVoiceCommandParser.parse("بازگشت"))
    }

    @Test fun ambiguousOrLongSentencesDoNotAccidentallyConfirm() {
        assertEquals(VoiceIntent.UNKNOWN, PersianVoiceCommandParser.parse("شاید بله"))
        assertEquals(VoiceIntent.UNKNOWN, PersianVoiceCommandParser.parse(""))
        assertEquals(VoiceIntent.UNKNOWN, PersianVoiceCommandParser.parse("انجام بده"))
    }
}
