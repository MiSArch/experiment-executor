package org.misarch.experimentexecutor.plugin.failure

import java.util.UUID

interface FailurePluginInterface {

    suspend fun initializeFailure(testUUID: UUID)

    suspend fun startTimedExperiment(testUUID: UUID)

    suspend fun stopExperiment(testUUID: UUID)
}