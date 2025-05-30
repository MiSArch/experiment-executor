package org.misarch.experimentexecutor.plugin.failure

import org.misarch.experimentexecutor.model.Failure
import java.util.UUID

interface FailurePluginInterface {

    suspend fun initializeFailure(failure: Failure, testUUID: UUID)

    suspend fun startTimedExperiment(testUUID: UUID)

    suspend fun stopExperiment(testUUID: UUID)
}