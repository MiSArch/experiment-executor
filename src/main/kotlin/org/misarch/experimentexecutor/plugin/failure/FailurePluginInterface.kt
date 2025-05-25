package org.misarch.experimentexecutor.plugin.failure

import org.misarch.experimentexecutor.model.Failure
import java.util.UUID

interface FailurePluginInterface {

    suspend fun initalizeFailure(failure: Failure, testUUID: UUID)

    suspend fun startTimedExperiment()
}