package com.rescueguide.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionFlowTest {
    @Test fun moduleChoiceStartsFreshSelection() {
        val state = SessionFlowReducer.reduce(
            SessionFlowState(), SessionFlowAction.ChooseModule(AppModule.CHOKING)
        )
        assertEquals(SessionPage.SELECTION, state.page)
        assertEquals(AppModule.CHOKING, state.selection?.module)
        assertFalse(state.selection!!.isComplete)
    }

    @Test fun cannotContinueUntilBothChoicesExist() {
        val start = SessionFlowReducer.reduce(
            SessionFlowState(), SessionFlowAction.ChooseModule(AppModule.BASIC_LIFE_SUPPORT)
        )
        val blocked = SessionFlowReducer.reduce(start, SessionFlowAction.ContinueToReviewGate)
        assertEquals(SessionPage.SELECTION, blocked.page)
        val withPatient = SessionFlowReducer.reduce(
            start, SessionFlowAction.ChoosePatient(PatientGroup.INFANT)
        )
        val stillBlocked = SessionFlowReducer.reduce(withPatient, SessionFlowAction.ContinueToReviewGate)
        assertEquals(SessionPage.SELECTION, stillBlocked.page)
    }

    @Test fun completeSelectionOnlyReachesReviewGateNotClinicalSession() {
        var state = SessionFlowReducer.reduce(
            SessionFlowState(), SessionFlowAction.ChooseModule(AppModule.BASIC_LIFE_SUPPORT)
        )
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ChoosePatient(PatientGroup.CHILD))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ChooseRescuer(RescuerType.TRAINED_RESCUER))
        assertTrue(state.selection!!.isComplete)
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ContinueToReviewGate)
        assertEquals(SessionPage.REVIEW_GATE, state.page)
    }

    @Test fun backFromGatePreservesChoicesAndBackFromSelectionReturnsHome() {
        var state = SessionFlowState(page = SessionPage.SELECTION,
            selection = SessionSelection(AppModule.CHOKING, PatientGroup.ADULT, RescuerType.LAY_RESCUER))
        state = SessionFlowReducer.reduce(state, SessionFlowAction.ContinueToReviewGate)
        state = SessionFlowReducer.reduce(state, SessionFlowAction.Back)
        assertEquals(SessionPage.SELECTION, state.page)
        assertEquals(PatientGroup.ADULT, state.selection?.patientGroup)
        state = SessionFlowReducer.reduce(state, SessionFlowAction.Back)
        assertEquals(SessionFlowState(), state)
    }

    @Test fun resetClearsSelectionAndPage() {
        val state = SessionFlowState(SessionPage.REVIEW_GATE,
            SessionSelection(AppModule.CHOKING, PatientGroup.INFANT, RescuerType.LAY_RESCUER))
        assertEquals(SessionFlowState(), SessionFlowReducer.reduce(state, SessionFlowAction.Reset))
    }
}
