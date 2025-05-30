package org.misarch.experimentexecutor.plugin.failure.chaostoolkit

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import org.misarch.experimentexecutor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.File
import java.util.UUID

private val logger = KotlinLogging.logger { }

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

    override suspend fun startTimedExperiment(testUUID: UUID) {}

    override suspend fun stopExperiment(testUUID: UUID) {
        logger.info ("Stopping Chaos Toolkit experiment for testUUID: $testUUID")
        webClient.post()
            .uri("$chaosToolkitExecutorHost/stop-experiment?testUUID=$testUUID")
            .retrieve()
            .onStatus({ it.value() == 404 }) { Mono.empty() }
            .toBodilessEntity()
            .awaitSingle()
    }
}