package org.misarch.experimentexecutor.plugin.workload.gatling

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withTimeout
import org.misarch.experimentexecutor.controller.experiment.model.EncodedFileDTO
import org.misarch.experimentexecutor.model.SteadyState
import org.misarch.experimentexecutor.model.WarmUp
import org.misarch.experimentexecutor.plugin.failure.misarch.withRetries
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.File
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val logger = KotlinLogging.logger { }

@OptIn(ExperimentalEncodingApi::class)
class GatlingPlugin(
    private val webClient: WebClient,
    private val gatlingExecutorHost: String,
    private val basePath: String,
) : WorkloadPluginInterface {
    override suspend fun executeWorkLoad(
        testUUID: UUID,
        testVersion: String,
        warmUp: WarmUp?,
        steadyState: SteadyState?,
    ) {
        val files = File("$basePath/$testUUID/$testVersion")
        val fileNames =
            files
                .listFiles()
                ?.filter { it.isFile && it.name.endsWith(".kt") }
                ?.map { it.name }
                ?: emptyList()

        val gatlingConfigs =
            fileNames.map { workFileName ->
                val fileName = workFileName.removeSuffix(".kt")
                val userStepsFileName = "$fileName.csv"
                val workFileContent = File("$basePath/$testUUID/$testVersion/$workFileName").readText()
                val userStepsFileContent = File("$basePath/$testUUID/$testVersion/$userStepsFileName").readText()
                EncodedFileDTO(
                    fileName = fileName,
                    encodedWorkFileContent = Base64.encode(workFileContent.toByteArray(Charsets.UTF_8)),
                    encodedUserStepsFileContent = Base64.encode(userStepsFileContent.toByteArray(Charsets.UTF_8)),
                )
            }

        withRetries(maxRetries = 6, initialDelayMillis = 500) {
            withTimeout(1500) {
                webClient
                    .post()
                    .uri(
                        "$gatlingExecutorHost/start-experiment" +
                            "?testUUID=$testUUID" +
                            "&testVersion=$testVersion" +
                            "&warmUp=${(warmUp != null)}" +
                            "&warmUpRate=${warmUp?.rate ?: 0}" +
                            "&warmUpDuration=${warmUp?.duration ?: 0}" +
                            "&steadyState=${(steadyState != null)}" +
                            "&steadyStateRate=${steadyState?.rate ?: 0}" +
                            "&steadyStateDuration=${steadyState?.duration ?: 0}",
                    ).bodyValue(gatlingConfigs)
                    .retrieve()
                    .toBodilessEntity()
                    .awaitSingle()
            }
        }
    }

    override suspend fun stopWorkLoad(
        testUUID: UUID,
        testVersion: String,
    ) {
        logger.info { "Stopping gatling workload for testUUID: $testUUID and testVersion: $testVersion" }
        webClient
            .post()
            .uri("$gatlingExecutorHost/stop-experiment?testUUID=$testUUID&testVersion=$testVersion")
            .retrieve()
            .onStatus({ it.value() == 404 }) { Mono.empty() }
            .toBodilessEntity()
            .awaitSingle()
    }
}
