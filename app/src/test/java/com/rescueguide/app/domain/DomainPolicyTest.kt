package com.rescueguide.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainPolicyTest {
    @Test fun iranEmergencyNumberIsDialable() {
        assertTrue(EmergencyDirectory.isDialable(EmergencyDirectory.iran.emergencyNumber))
    }

    @Test fun malformedNumbersAreRejected() {
        assertFalse(EmergencyDirectory.isDialable("115;123"))
        assertFalse(EmergencyDirectory.isDialable("abc"))
        assertFalse(EmergencyDirectory.isDialable("1"))
    }

    @Test fun clinicalBundleRequiresRecordedApproval() {
        assertFalse(ClinicalBundleStatus().canActivate)
        assertTrue(ClinicalBundleStatus(ClinicalApprovalState.APPROVED, "Reviewer", "2026-11-12T00:00:00Z").canActivate)
        assertFalse(ClinicalBundleStatus(ClinicalApprovalState.APPROVED, null, "2026-11-12T00:00:00Z").canActivate)
    }
}
