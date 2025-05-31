package org.misarch.experimentexecutor.service.model

import org.misarch.experimentexecutor.model.Goal
import java.util.UUID

data class ExperimentState(
    val testUUID: UUID,
    val testVersion: String,
    val triggerState: TriggerState,
    val startTime: String? = null,
    val endTime: String? = null,
    val goals: List<Goal>,
) {
    enum class TriggerState {
        INITIALIZING,
        STARTED,
        COMPLETED,
    }
}
