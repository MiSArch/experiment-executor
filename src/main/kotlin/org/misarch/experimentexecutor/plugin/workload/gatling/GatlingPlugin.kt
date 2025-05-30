package org.misarch.experimentexecutor.plugin.workload.gatling

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.reactor.awaitSingle
import org.misarch.experimentexecutor.config.*
import org.misarch.experimentexecutor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.File
import java.util.UUID

class GatlingPlugin(
    private val webClient: WebClient,
    private val tokenConfig: TokenConfig,
    private val gatlingExecutorHost: String,
    private val triggerDelay: Long,
) : WorkloadPluginInterface {

    override suspend fun executeWorkLoad(workLoad: WorkLoad, testUUID: UUID) {
        val token = getOAuthAccessToken(
            clientId = tokenConfig.clientId,
            url = "${tokenConfig.host}/${tokenConfig.path}",
            username = tokenConfig.username,
            password = tokenConfig.password,
        )
        val userSteps = File(workLoad.gatling.userStepsPathUri).readText()
        webClient.post()
            .uri(
                "$gatlingExecutorHost/start-experiment?testUUID=$testUUID&accessToken=$token&triggerDelay=$triggerDelay&targetUrl=${workLoad.gatling.endpointHost}"
            ).bodyValue(userSteps)
            .retrieve()
            .toBodilessEntity()
            .awaitSingle()
    }

    override suspend fun stopWorkLoad(testUUID: UUID) {
        webClient.post()
            .uri("$gatlingExecutorHost/stop-experiment?testUUID=$testUUID")
            .retrieve()
            .onStatus({ it.value() == 404 }) { Mono.empty() }
            .toBodilessEntity()
            .awaitSingle()
    }

    private suspend fun getOAuthAccessToken(clientId: String, url: String, username: String, password: String): String {
        val data = "grant_type=password&client_id=$clientId&username=$username&password=$password"
        val response = webClient.post()
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