package org.misarch.experimentexecutor.plugin.workload

import org.misarch.experimentexecutor.executor.model.WorkLoad

interface WorkloadPluginInterface {
    suspend fun executeWorkLoad(workLoad: WorkLoad): Boolean
}