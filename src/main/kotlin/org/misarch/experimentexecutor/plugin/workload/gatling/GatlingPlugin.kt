package org.misarch.experimentexecutor.plugin.workload.gatling

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import org.misarch.experimentexecutor.controller.experiment.model.EncodedFileDTO
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

        webClient
            .post()
            .uri(
                "$gatlingExecutorHost/start-experiment" +
                    "?testUUID=$testUUID" +
                    "&testVersion=$testVersion",
            ).bodyValue(gatlingConfigs)
            .retrieve()
            .toBodilessEntity()
            .awaitSingle()
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
