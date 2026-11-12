package com.rescueguide.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientRoutingTest {
    @Test fun infantIsUnderOneYear() {
        assertEquals(PatientGroup.INFANT, PatientRouting.fromCompletedYears(0))
    }

    @Test fun oneYearOldIsChild() {
        assertEquals(PatientGroup.CHILD, PatientRouting.fromCompletedYears(1))
    }

    @Test fun pubertyRoutesToAdultRegardlessOfAgeNumber() {
        assertEquals(PatientGroup.ADULT, PatientRouting.fromCompletedYears(10, true))
    }

    @Test fun noPubertyRoutesToChild() {
        assertEquals(PatientGroup.CHILD, PatientRouting.fromCompletedYears(10, false))
    }

    @Test fun unknownPubertyDoesNotGuessForOlderChild() {
        assertEquals(null, PatientRouting.fromCompletedYears(10, null))
    }

    @Test fun invalidNegativeAgeRejected() {
        assertEquals(null, PatientRouting.fromCompletedYears(-1))
    }

    @Test fun selectionNeedsBothFields() {
        val incomplete = SessionSelection(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.ADULT)
        assertFalse(incomplete.isComplete)
        assertTrue(incomplete.copy(rescuerType = RescuerType.LAY_RESCUER).isComplete)
    }
}
