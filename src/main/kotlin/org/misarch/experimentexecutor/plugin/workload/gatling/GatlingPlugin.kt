package org.misarch.experimentexecutor.plugin.workload.gatling

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import org.misarch.experimentexecutor.config.TokenConfig
import org.misarch.experimentexecutor.controller.experiment.model.EncodedFileDTO
import org.misarch.experimentexecutor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.springframework.http.MediaType
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
    private val tokenConfig: TokenConfig,
    private val gatlingExecutorHost: String,
    private val basePath: String,
) : WorkloadPluginInterface {
    override suspend fun executeWorkLoad(
        workLoad: WorkLoad,
        testUUID: UUID,
        testVersion: String,
    ) {
        val token =
            workLoad.gatling.endpointAccessToken ?: getOAuthAccessToken(
                clientId = tokenConfig.clientId,
                url = "${tokenConfig.host}/${tokenConfig.path}",
                username = tokenConfig.username,
                password = tokenConfig.password,
            )

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
                    "&testVersion=$testVersion" +
                    "&accessToken=$token" +
                    "&targetUrl=${workLoad.gatling.endpointHost}",
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

    private suspend fun getOAuthAccessToken(
        clientId: String,
        url: String,
        username: String,
        password: String,
    ): String {
        val data = "grant_type=password&client_id=$clientId&username=$username&password=$password"
        val response =
            webClient
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(data)
                .retrieve()
                .bodyToMono(TokenResponse::class.java)
                .awaitSingle()

        return response.accessToken
    }
}

data class TokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("not-before-policy")
    val notBeforePolicy: Int,
    @JsonProperty("session_state")
    val sessionState: String,
    @JsonProperty("scope")
    val scope: String,
)
