package com.rescueguide.app.domain

/** Navigation-only state. It deliberately has no authority to start clinical guidance. */
enum class SessionPage { HOME, SELECTION, REVIEW_GATE, ACTIVE_PATH }

data class SessionFlowState(
    val page: SessionPage = SessionPage.HOME,
    val selection: SessionSelection? = null,
    val activeStepIndex: Int = 0
)

sealed interface SessionFlowAction {
    data class ChooseModule(val module: AppModule) : SessionFlowAction
    data class ChoosePatient(val group: PatientGroup) : SessionFlowAction
    data class ChooseRescuer(val type: RescuerType) : SessionFlowAction
    data class ChooseRescueBreaths(val canProvide: Boolean) : SessionFlowAction
    data class ChooseTrainedRescuerCount(val count: Int) : SessionFlowAction
    data object ContinueToReviewGate : SessionFlowAction
    data object StartPath : SessionFlowAction
    data object NextStep : SessionFlowAction
    data object PreviousStep : SessionFlowAction
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
        is SessionFlowAction.ChooseRescueBreaths -> if (state.page == SessionPage.SELECTION) {
            state.copy(selection = state.selection?.copy(canProvideRescueBreaths = action.canProvide))
        } else state
        is SessionFlowAction.ChooseTrainedRescuerCount -> if (state.page == SessionPage.SELECTION && action.count in 1..2) {
            state.copy(selection = state.selection?.copy(trainedRescuerCount = action.count))
        } else state
        SessionFlowAction.ContinueToReviewGate -> if (
            state.page == SessionPage.SELECTION && state.selection?.isComplete == true
        ) state.copy(page = SessionPage.REVIEW_GATE) else state
        SessionFlowAction.StartPath -> if (state.page == SessionPage.REVIEW_GATE && state.selection?.isComplete == true) state.copy(page = SessionPage.ACTIVE_PATH, activeStepIndex = 0) else state
        SessionFlowAction.NextStep -> if (state.page == SessionPage.ACTIVE_PATH) state.copy(activeStepIndex = state.activeStepIndex + 1) else state
        SessionFlowAction.PreviousStep -> if (state.page == SessionPage.ACTIVE_PATH) state.copy(activeStepIndex = (state.activeStepIndex - 1).coerceAtLeast(0)) else state
        SessionFlowAction.Back -> when (state.page) {
            SessionPage.HOME -> state
            SessionPage.SELECTION -> SessionFlowState()
            SessionPage.REVIEW_GATE -> state.copy(page = SessionPage.SELECTION)
            SessionPage.ACTIVE_PATH -> state.copy(page = SessionPage.REVIEW_GATE, activeStepIndex = 0)
        }
        SessionFlowAction.Reset -> SessionFlowState()
    }
}
