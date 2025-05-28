package org.misarch.experimentexecutor.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.misarch.experimentexecutor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.ChaosToolkitPlugin
import org.misarch.experimentexecutor.plugin.failure.misarch.MisarchExperimentConfigPlugin
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Service
class ExperimentFailureService(
    webClient: WebClient,
    @Value("\${misarch.experiment-config.host}") private val misarchExperimentConfigHost: String,
    @Value("\${chaostoolkit.executor-host}") private val chaosToolkitExecutorHost : String,
) {

    val registry = listOf(
        ChaosToolkitPlugin(webClient, chaosToolkitExecutorHost),
        MisarchExperimentConfigPlugin(webClient, misarchExperimentConfigHost),
    )

    suspend fun setupExperimentFailure(failure: Failure, testUUID: UUID) {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.initalizeFailure(failure, testUUID) }
            }
        }.awaitAll()
    }

    suspend fun startExperimentFailure() {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.startTimedExperiment() }
            }
        }.awaitAll()
    }
}