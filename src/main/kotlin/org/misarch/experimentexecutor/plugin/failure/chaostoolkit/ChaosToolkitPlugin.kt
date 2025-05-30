package org.misarch.experimentexecutor.plugin.failure.chaostoolkit

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import org.misarch.experimentexecutor.config.CHAOSTOOLKIT_FILENAME
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.File
import java.util.UUID

private val logger = KotlinLogging.logger { }

class ChaosToolkitPlugin(private val webClient: WebClient, private val chaosToolkitExecutorHost: String, private val basePath: String,) : FailurePluginInterface {
    override suspend fun initializeFailure(testUUID: UUID) {
        val experimentYaml = File("$basePath/$testUUID/$CHAOSTOOLKIT_FILENAME").readText()
        webClient.post()
            .uri("$chaosToolkitExecutorHost/start-experiment?testUUID=$testUUID")
            .bodyValue(experimentYaml)
            .retrieve()
            .toBodilessEntity()
            .awaitSingle()
    }

    override suspend fun startTimedExperiment(testUUID: UUID) {}

    override suspend fun stopExperiment(testUUID: UUID) {
        logger.info { "Stopping Chaos Toolkit experiment for testUUID: $testUUID" }
        webClient.post()
            .uri("$chaosToolkitExecutorHost/stop-experiment?testUUID=$testUUID")
            .retrieve()
            .onStatus({ it.value() == 404 }) { Mono.empty() }
            .toBodilessEntity()
            .awaitSingle()
    }
}