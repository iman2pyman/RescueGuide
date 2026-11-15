package com.rescueguide.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionFlowTest {
    @Test fun moduleChoiceStartsFreshSelection() {
        val state = SessionFlowReducer.reduce(SessionFlowState(), SessionFlowAction.ChooseModule(AppModule.BASIC_LIFE_SUPPORT))
        assertEquals(SessionPage.SELECTION, state.page)
        assertEquals(AppModule.BASIC_LIFE_SUPPORT, state.selection?.module)
    }

    @Test fun choosingPatientAndRescuerStartsPathWithoutReviewGate() {
        var state = SessionFlowReducer.reduce(SessionFlowState(), SessionFlowAction.ChooseModule(AppModule.BASIC_LIFE_SUPPORT))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ChoosePatient(PatientGroup.INFANT))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ChooseRescuer(RescuerType.LAY_RESCUER))
        assertEquals(SessionPage.ACTIVE_PATH, state.page)
        assertEquals(PatientGroup.INFANT, state.selection?.patientGroup)
        assertEquals(false, state.selection?.canProvideRescueBreaths)
        assertTrue(state.selection!!.isComplete)
    }

    @Test fun trainedRescuerUsesVentilationPathByDefault() {
        var state = SessionFlowReducer.reduce(SessionFlowState(), SessionFlowAction.ChooseModule(AppModule.BASIC_LIFE_SUPPORT))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ChoosePatient(PatientGroup.CHILD))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ChooseRescuer(RescuerType.TRAINED_RESCUER))
        assertEquals(SessionPage.ACTIVE_PATH, state.page)
        assertEquals(true, state.selection?.canProvideRescueBreaths)
    }

    @Test fun activePathCanMoveForwardAndBack() {
        var state = SessionFlowReducer.reduce(SessionFlowState(), SessionFlowAction.ChooseModule(AppModule.BASIC_LIFE_SUPPORT))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ChoosePatient(PatientGroup.ADULT))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ChooseRescuer(RescuerType.LAY_RESCUER))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.NextStep)
        assertEquals(1, state.activeStepIndex)
        state = SessionFlowReducer.reduce(state, SessionFlowAction.PreviousStep)
        assertEquals(0, state.activeStepIndex)
    }

    @Test fun backFromActiveReturnsToRescuerChoiceThenHome() {
        var state = SessionFlowReducer.reduce(SessionFlowState(), SessionFlowAction.ChooseModule(AppModule.BASIC_LIFE_SUPPORT))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ChoosePatient(PatientGroup.ADULT))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ChooseRescuer(RescuerType.LAY_RESCUER))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.Back)
        assertEquals(SessionPage.SELECTION, state.page)
        state = SessionFlowReducer.reduce(state, SessionFlowAction.Back)
        assertEquals(SessionFlowState(), state)
    }

    @Test fun resetClearsFlow() {
        val state = SessionFlowState(page = SessionPage.ACTIVE_PATH)
        assertEquals(SessionFlowState(), SessionFlowReducer.reduce(state, SessionFlowAction.Reset))
    }
}
