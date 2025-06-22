package org.misarch.experimentexecutor.plugin.workload

import java.util.UUID

interface WorkloadPluginInterface {
    suspend fun executeWorkLoad(
        testUUID: UUID,
        testVersion: String,
    )

    suspend fun stopWorkLoad(
        testUUID: UUID,
        testVersion: String,
    )
}
