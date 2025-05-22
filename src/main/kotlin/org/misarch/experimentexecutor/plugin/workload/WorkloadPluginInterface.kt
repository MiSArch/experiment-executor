package org.misarch.experimentexecutor.plugin.workload

import org.misarch.experimentexecutor.executor.model.WorkLoad
import java.util.UUID

interface WorkloadPluginInterface {
    suspend fun executeWorkLoad(workLoad: WorkLoad, testUUID: UUID): Boolean
}