package com.rescueguide.app.voice

import org.junit.Assert.assertEquals
import org.junit.Test
import com.rescueguide.app.domain.PatientGroup

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
        assertEquals(VoiceIntent.ROSC, PersianVoiceCommandParser.parse("تنفس طبیعی برگشت"))
        assertEquals(VoiceIntent.ROSC, PersianVoiceCommandParser.parse("علائم حیاتی برگشت"))
    }

    @Test fun routesExplicitAgeWordsAndDoesNotGuessPubertyBoundary() {
        assertEquals(PatientGroup.INFANT, PersianVoiceCommandParser.parsePatientGroup("شیرخوار"))
        assertEquals(PatientGroup.INFANT, PersianVoiceCommandParser.parsePatientGroup("۸ ماهه"))
        assertEquals(PatientGroup.CHILD, PersianVoiceCommandParser.parsePatientGroup("کودک"))
        assertEquals(PatientGroup.CHILD, PersianVoiceCommandParser.parsePatientGroup("۷ ساله قبل از بلوغ"))
        assertEquals(null, PersianVoiceCommandParser.parsePatientGroup("۷ ساله"))
        assertEquals(PatientGroup.ADULT, PersianVoiceCommandParser.parsePatientGroup("بزرگسال"))
        assertEquals(PatientGroup.ADULT, PersianVoiceCommandParser.parsePatientGroup("۱۸ ساله"))
        assertEquals(null, PersianVoiceCommandParser.parsePatientGroup("۱۴ ساله"))
    }

    @Test fun ambiguousOrLongSentencesDoNotAccidentallyConfirm() {
        assertEquals(VoiceIntent.UNKNOWN, PersianVoiceCommandParser.parse("شاید بله"))
        assertEquals(VoiceIntent.UNKNOWN, PersianVoiceCommandParser.parse(""))
        assertEquals(VoiceIntent.UNKNOWN, PersianVoiceCommandParser.parse("انجام بده"))
    }
}
