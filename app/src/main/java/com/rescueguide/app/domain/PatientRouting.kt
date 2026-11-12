package com.rescueguide.app.domain

/** Explicit routing choices; choosing a group never starts a clinical protocol by itself. */
object PatientRouting {
    /** Returns null for unknown/invalid age. Age is measured in completed years. */
    fun fromCompletedYears(age: Int, signsOfPuberty: Boolean? = null): PatientGroup? {
        if (age < 0) return null
        if (age == 0) return PatientGroup.INFANT
        if (signsOfPuberty == true) return PatientGroup.ADULT
        if (signsOfPuberty == false) return PatientGroup.CHILD
        // Without a puberty assessment, age alone cannot safely decide the child/adult boundary.
        return if (age == 1) PatientGroup.CHILD else null
    }

    fun label(group: PatientGroup): String = when (group) {
        PatientGroup.ADULT -> "بزرگسال"
        PatientGroup.CHILD -> "کودک"
        PatientGroup.INFANT -> "شیرخوار زیر یک سال"
    }
}

data class SessionSelection(
    val module: AppModule,
    val patientGroup: PatientGroup? = null,
    val rescuerType: RescuerType? = null,
    val canProvideRescueBreaths: Boolean? = null,
    val trainedRescuerCount: Int = 1
) {
    val isComplete: Boolean get() = patientGroup != null && rescuerType != null &&
        canProvideRescueBreaths != null && trainedRescuerCount in 1..2
}
