package com.rescueguide.app.clinical

import com.rescueguide.app.domain.AppModule
import com.rescueguide.app.domain.PatientGroup
import com.rescueguide.app.domain.RescuerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalPathwaysFaTest {
    @Test fun everyModuleAndAgeCombinationHasAPath() {
        AppModule.values().forEach { module ->
            PatientGroup.values().forEach { patient ->
                RescuerType.values().forEach { rescuer ->
                    assertTrue(ClinicalPathwaysFa.get(module, patient, rescuer).steps.isNotEmpty())
                }
            }
        }
    }

    @Test fun infantBlsUsesTwoThumbOrHeelAndAvoidsTwoFingerMethod() {
        val text = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.INFANT, RescuerType.TRAINED_RESCUER)
            .steps.joinToString(" ") { it.instruction }
        assertTrue(text.contains("دو شست"))
        assertTrue(text.contains("پاشنه یک دست"))
        assertTrue(text.contains("دو انگشتی استفاده نکنید"))
    }

    @Test fun trainedAdultWithPulseButNoBreathingGetsRescueBreathPath() {
        val path = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.ADULT, RescuerType.TRAINED_RESCUER, true)
        assertTrue(path.steps.any { it.instruction.contains("هر ۶ ثانیه یک تنفس") })
    }

    @Test fun trainedChildAndInfantIncludeVentilationAndBradycardiaBranch() {
        val child = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.CHILD, RescuerType.TRAINED_RESCUER, true)
        val infant = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.INFANT, RescuerType.TRAINED_RESCUER, true)
        assertTrue(child.steps.any { it.instruction.contains("هر ۲ تا ۳ ثانیه") && it.instruction.contains("کمتر از ۶۰") })
        assertTrue(infant.steps.any { it.instruction.contains("هر ۲ تا ۳ ثانیه") && it.instruction.contains("کمتر از ۶۰") })
    }

    @Test fun infantChokingNeverUsesAbdominalThrusts() {
        val text = ClinicalPathwaysFa.get(AppModule.CHOKING, PatientGroup.INFANT, RescuerType.LAY_RESCUER)
            .steps.joinToString(" ") { it.instruction }
        assertTrue(text.contains("هرگز فشار شکمی وارد نکنید"))
    }

    @Test fun pregnancyChokingUsesChestThrustAlternative() {
        val text = ClinicalPathwaysFa.get(AppModule.CHOKING, PatientGroup.ADULT, RescuerType.LAY_RESCUER)
            .steps.joinToString(" ") { it.instruction }
        assertTrue(text.contains("باردار"))
        assertTrue(text.contains("فشار قفسه سینه"))
    }

    @Test fun childTwoTrainedRescuersUseFifteenToTwoWhenBreathsAreProvided() {
        val path = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.CHILD, RescuerType.TRAINED_RESCUER, true, 2)
        assertTrue(path.steps.any { it.instruction.contains("۱۵ به ۲") })
        val infant = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.INFANT, RescuerType.TRAINED_RESCUER, true, 2)
        assertTrue(infant.steps.any { it.instruction.contains("۱۵ به ۲") })
    }

    @Test fun childSingleRescuerUsesThirtyToTwoAndNoBreathsUsesCompressionOnly() {
        val one = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.CHILD, RescuerType.TRAINED_RESCUER, true, 1)
        assertTrue(one.steps.any { it.instruction.contains("۳۰ به ۲") })
        val compressionOnly = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.CHILD, RescuerType.LAY_RESCUER, false, 1)
        assertTrue(compressionOnly.steps.any { it.instruction.contains("فشارهای پیوسته") })
    }

    @Test fun pediatricPathsEmphasizeLessThanTenSecondCompressionPauses() {
        val child = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.CHILD, RescuerType.LAY_RESCUER, false)
        val infant = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.INFANT, RescuerType.LAY_RESCUER, false)
        assertTrue(child.steps.any { it.instruction.contains("کمتر از ۱۰ ثانیه") })
        assertTrue(infant.steps.any { it.instruction.contains("کمتر از ۱۰ ثانیه") })
    }

    @Test fun everyPathHasAReportedReturnOfNormalBreathingStep() {
        AppModule.values().forEach { module ->
            PatientGroup.values().forEach { patient ->
                val path = ClinicalPathwaysFa.get(module, patient, RescuerType.LAY_RESCUER, false)
                assertTrue(path.steps.any { it.id == "rosc" })
            }
        }
    }

    @Test fun allActionStepsAreMarkedTimeCritical() {
        AppModule.values().forEach { module ->
            PatientGroup.values().forEach { patient ->
                val path = ClinicalPathwaysFa.get(module, patient, RescuerType.LAY_RESCUER)
                assertFalse(path.steps.isEmpty())
                assertEquals(path.steps.size, path.steps.count { it.timeCritical })
            }
        }
    }
}
