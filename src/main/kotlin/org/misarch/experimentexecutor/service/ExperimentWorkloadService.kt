package org.misarch.experimentexecutor.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.misarch.experimentexecutor.model.SteadyState
import org.misarch.experimentexecutor.model.WarmUp
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.misarch.experimentexecutor.plugin.workload.gatling.GatlingPlugin
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

@Service
class ExperimentWorkloadService(
    webClient: WebClient,
    @Value("\${gatling.executor-host}") private val gatlingExecutorHost: String,
    @Value("\${experiment-executor.base-path}") private val basePath: String,
) {
    val registry =
        listOf<WorkloadPluginInterface>(
            GatlingPlugin(webClient, gatlingExecutorHost, basePath),
        )

    suspend fun executeWorkLoad(
        testUUID: UUID,
        testVersion: String,
        warmUp: WarmUp? = null,
        steadyState: SteadyState? = null,
    ) {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.executeWorkLoad(testUUID, testVersion, warmUp, steadyState) }
            }
        }.awaitAll()
    }

    suspend fun stopWorkLoad(
        testUUID: UUID,
        testVersion: String,
    ) {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.stopWorkLoad(testUUID, testVersion) }
            }
        }.awaitAll()
    }
}
