package org.misarch.experimentexecutor.plugin.failure.chaostoolkit

import kotlinx.coroutines.reactor.awaitSingle
import org.misarch.experimentexecutor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import org.springframework.web.reactive.function.client.WebClient
import java.io.File
import java.util.UUID

class ChaosToolkitPlugin(private val webClient: WebClient, private val chaosToolkitExecutorHost: String) : FailurePluginInterface {
    override suspend fun initializeFailure(failure: Failure, testUUID: UUID) {
        val experimentYaml = File(failure.chaosToolkit.pathUri).readText()
        webClient.post()
            .uri("$chaosToolkitExecutorHost/start-experiment?testUUID=$testUUID")
            .bodyValue(experimentYaml)
            .retrieve()
            .toBodilessEntity()
            .awaitSingle()
    }

    override suspend fun startTimedExperiment() {}
}