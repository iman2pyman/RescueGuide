package com.rescueguide.app.domain

/** Navigation-only state. It deliberately has no authority to start clinical guidance. */
enum class SessionPage { HOME, SELECTION, REVIEW_GATE }

data class SessionFlowState(
    val page: SessionPage = SessionPage.HOME,
    val selection: SessionSelection? = null
)

sealed interface SessionFlowAction {
    data class ChooseModule(val module: AppModule) : SessionFlowAction
    data class ChoosePatient(val group: PatientGroup) : SessionFlowAction
    data class ChooseRescuer(val type: RescuerType) : SessionFlowAction
    data object ContinueToReviewGate : SessionFlowAction
    data object Back : SessionFlowAction
    data object Reset : SessionFlowAction
}

object SessionFlowReducer {
    fun reduce(state: SessionFlowState, action: SessionFlowAction): SessionFlowState = when (action) {
        is SessionFlowAction.ChooseModule -> SessionFlowState(
            page = SessionPage.SELECTION,
            selection = SessionSelection(module = action.module)
        )
        is SessionFlowAction.ChoosePatient -> if (state.page == SessionPage.SELECTION) {
            state.copy(selection = state.selection?.copy(patientGroup = action.group))
        } else state
        is SessionFlowAction.ChooseRescuer -> if (state.page == SessionPage.SELECTION) {
            state.copy(selection = state.selection?.copy(rescuerType = action.type))
        } else state
        SessionFlowAction.ContinueToReviewGate -> if (
            state.page == SessionPage.SELECTION && state.selection?.isComplete == true
        ) state.copy(page = SessionPage.REVIEW_GATE) else state
        SessionFlowAction.Back -> when (state.page) {
            SessionPage.HOME -> state
            SessionPage.SELECTION -> SessionFlowState()
            SessionPage.REVIEW_GATE -> state.copy(page = SessionPage.SELECTION)
        }
        SessionFlowAction.Reset -> SessionFlowState()
    }
}
