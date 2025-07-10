package org.misarch.experimentexecutor.plugin.workload

import org.misarch.experimentexecutor.model.SteadyState
import org.misarch.experimentexecutor.model.WarmUp
import java.util.UUID

interface WorkloadPluginInterface {
    suspend fun executeWorkLoad(
        testUUID: UUID,
        testVersion: String,
        warmUp: WarmUp? = null,
        steadyState: SteadyState? = null,
    )

    suspend fun stopWorkLoad(
        testUUID: UUID,
        testVersion: String,
    )
}
