package com.rescueguide.app.domain

/** Clinical pathways stay unavailable until the clinical bundle is reviewed and approved. */
enum class AppModule { BASIC_LIFE_SUPPORT, CHOKING }
enum class PatientGroup { ADULT, CHILD, INFANT }
enum class RescuerType { LAY_RESCUER, TRAINED_RESCUER }

data class CountryEmergencyConfig(
    val countryCode: String,
    val displayNameFa: String,
    val emergencyNumber: String,
    val enabled: Boolean
)

object EmergencyDirectory {
    val iran = CountryEmergencyConfig(
        countryCode = "IR",
        displayNameFa = "ایران",
        emergencyNumber = "115",
        enabled = true
    )

    fun isDialable(number: String): Boolean = number.matches(Regex("\\+?[0-9]{2,15}"))
}

enum class ClinicalApprovalState { NOT_REVIEWED, APPROVED, REJECTED }

data class ClinicalBundleStatus(
    val state: ClinicalApprovalState = ClinicalApprovalState.NOT_REVIEWED,
    val reviewer: String? = null,
    val reviewedAtIso: String? = null
) {
    val canActivate: Boolean
        get() = state == ClinicalApprovalState.APPROVED &&
            !reviewer.isNullOrBlank() && !reviewedAtIso.isNullOrBlank()
}
