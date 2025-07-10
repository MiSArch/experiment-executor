package org.misarch.experimentexecutor.plugin.failure

import java.util.UUID

interface FailurePluginInterface {
    suspend fun initializeFailure(
        testUUID: UUID,
        testVersion: String,
        testDelay: Int,
    )

    suspend fun stopExperiment(
        testUUID: UUID,
        testVersion: String,
    )
}
