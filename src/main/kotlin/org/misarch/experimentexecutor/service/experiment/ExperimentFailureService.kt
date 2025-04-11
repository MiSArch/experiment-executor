package org.misarch.experimentexecutor.service.experiment

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.misarch.experimentexecutor.executor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.ChaosToolkitPlugin
import org.misarch.experimentexecutor.plugin.failure.misarch.MisarchExperimentConfigPlugin
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class ExperimentFailureService(webClient: WebClient) {

    // TODO implement a plugin registry based on a configuration file
    val registry = listOf(
        ChaosToolkitPlugin(),
        MisarchExperimentConfigPlugin(webClient),
    )

    suspend fun setupExperimentFailure(failure: Failure) {
        supervisorScope {
            registry.map { plugin ->
                async { plugin.executeFailure(failure) }
            }
        }.awaitAll()
    }

    suspend fun resetExperimentFailure() {
    }
}