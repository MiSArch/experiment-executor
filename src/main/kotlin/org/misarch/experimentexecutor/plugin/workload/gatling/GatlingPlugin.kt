package org.misarch.experimentexecutor.plugin.workload.gatling

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import org.misarch.experimentexecutor.config.GATLING_USERSTEPS_FILENAME
import org.misarch.experimentexecutor.config.TokenConfig
import org.misarch.experimentexecutor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.File
import java.util.UUID

private val logger = KotlinLogging.logger { }

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

        val userSteps = File("$basePath/$testUUID/$testVersion/$GATLING_USERSTEPS_FILENAME").readText()
        webClient
            .post()
            .uri(
                "$gatlingExecutorHost/start-experiment" +
                    "?testUUID=$testUUID" +
                    "&testVersion=$testVersion" +
                    "&accessToken=$token" +
                    "&targetUrl=${workLoad.gatling.endpointHost}",
            ).bodyValue(userSteps)
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
