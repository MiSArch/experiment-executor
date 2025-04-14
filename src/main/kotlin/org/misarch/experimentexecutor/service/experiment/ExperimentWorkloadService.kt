package org.misarch.experimentexecutor.service.experiment

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.misarch.experimentexecutor.executor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.misarch.experimentexecutor.plugin.workload.gatling.GatlingPlugin
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Service
class ExperimentWorkloadService(webClient: WebClient) {

    // TODO implement a plugin registry based on a configuration file
    val registry = listOf<WorkloadPluginInterface>(
        GatlingPlugin(webClient)
    )

    suspend fun executeWorkLoad(workLoad: WorkLoad, testUUID: UUID) {
        supervisorScope {
            registry.map { plugin ->
                async { plugin.executeWorkLoad(workLoad, testUUID) }
            }
        }.awaitAll()
    }
}