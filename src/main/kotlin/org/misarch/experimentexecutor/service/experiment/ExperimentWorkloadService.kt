package org.misarch.experimentexecutor.service.experiment

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.misarch.experimentexecutor.executor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.misarch.experimentexecutor.plugin.workload.gatling.GatlingPlugin
import org.springframework.stereotype.Service

@Service
class ExperimentWorkloadService {

    // TODO implement a plugin registry based on a configuration file
    val registry = listOf<WorkloadPluginInterface>(
        GatlingPlugin()
    )

    suspend fun executeWorkLoad(workLoad: WorkLoad) {
        supervisorScope {
            registry.map { plugin ->
                async { plugin.executeWorkLoad(workLoad) }
            }
        }.awaitAll()
    }
}