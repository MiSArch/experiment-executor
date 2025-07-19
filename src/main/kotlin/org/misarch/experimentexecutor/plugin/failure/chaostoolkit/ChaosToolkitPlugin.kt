package org.misarch.experimentexecutor.plugin.failure.chaostoolkit

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withTimeout
import org.misarch.experimentexecutor.config.CHAOSTOOLKIT_FILENAME
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import org.misarch.experimentexecutor.plugin.failure.misarch.withRetries
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.File
import java.util.UUID

private val logger = KotlinLogging.logger { }

class ChaosToolkitPlugin(
    private val webClient: WebClient,
    private val chaosToolkitExecutorHost: String,
    private val basePath: String,
) : FailurePluginInterface {
    override suspend fun initializeFailure(
        testUUID: UUID,
        testVersion: String,
        testDelay: Int,
    ) {
        val experimentYaml = File("$basePath/$testUUID/$testVersion/$CHAOSTOOLKIT_FILENAME").readText()
        withRetries {
            withTimeout(1500) {
                webClient
                    .post()
                    .uri("$chaosToolkitExecutorHost/start-experiment?testUUID=$testUUID&testVersion=$testVersion&testDelay=$testDelay")
                    .bodyValue(experimentYaml)
                    .retrieve()
                    .toBodilessEntity()
                    .awaitSingle()
            }
        }
    }

    override suspend fun stopExperiment(
        testUUID: UUID,
        testVersion: String,
    ) {
        logger.info { "Stopping Chaos Toolkit experiment for testUUID: $testUUID and testVersion: $testVersion" }
        webClient
            .post()
            .uri("$chaosToolkitExecutorHost/stop-experiment?testUUID=$testUUID&testVersion=$testVersion")
            .retrieve()
            .onStatus({ it.value() == 404 }) { Mono.empty() }
            .toBodilessEntity()
            .awaitSingle()
    }
}
