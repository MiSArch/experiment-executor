package org.misarch.experimentexecutor.plugin.workload

import org.misarch.experimentexecutor.model.WorkLoad
import java.util.UUID

interface WorkloadPluginInterface {
    suspend fun executeWorkLoad(workLoad: WorkLoad, testUUID: UUID): Boolean
}