package org.misarch.experimentexecutor.plugin.failure

import org.misarch.experimentexecutor.executor.model.Failure
import java.util.UUID

interface FailurePluginInterface {

    suspend fun executeFailure(failure: Failure, testUUID: UUID): Boolean

    suspend fun startExperiment(): Boolean
}