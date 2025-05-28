package org.misarch.experimentexecutor.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.misarch.experimentexecutor.config.TokenConfig
import org.misarch.experimentexecutor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.misarch.experimentexecutor.plugin.workload.gatling.GatlingPlugin
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Service
class ExperimentWorkloadService(
    webClient: WebClient,
    tokenConfig: TokenConfig,
    @Value("\${experiment-executor.url}") private val experimentExecutorHost: String,
) {

    val registry = listOf<WorkloadPluginInterface>(
        GatlingPlugin(webClient, tokenConfig, experimentExecutorHost)
    )

    suspend fun executeWorkLoad(workLoad: WorkLoad, testUUID: UUID) {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.executeWorkLoad(workLoad, testUUID) }
            }
        }.awaitAll()
    }
}