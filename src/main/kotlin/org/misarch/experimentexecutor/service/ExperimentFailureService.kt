package org.misarch.experimentexecutor.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.misarch.experimentexecutor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.ChaosToolkitPlugin
import org.misarch.experimentexecutor.plugin.failure.misarch.MisarchExperimentConfigPlugin
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Service
class ExperimentFailureService(webClient: WebClient,
    @Value("\${misarch.experiment-config.host}") private val misarchExperimentConfigHost: String) {

    // TODO implement a plugin registry based on a configuration file
    val registry = listOf(
        ChaosToolkitPlugin(),
        MisarchExperimentConfigPlugin(webClient, misarchExperimentConfigHost),
    )

    suspend fun setupExperimentFailure(failure: Failure, testUUID: UUID) {
        supervisorScope {
            registry.map { plugin ->
                async { plugin.initalizeFailure(failure, testUUID) }
            }
        }.awaitAll()
    }

    suspend fun startExperimentFailure() {
        supervisorScope {
            registry.map { plugin ->
                async { plugin.startTimedExperiment() }
            }
        }.awaitAll()
    }
}