package org.misarch.experimentexecutor.plugin.workload.gatling

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.misarch.experimentexecutor.config.*
import org.misarch.experimentexecutor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.util.DockerExecutor
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

class GatlingPlugin(
    private val webClient: WebClient,
    private val tokenConfig: TokenConfig,
    private val experimentExecutorHost: String,
) : WorkloadPluginInterface {

    override suspend fun executeWorkLoad(workLoad: WorkLoad, testUUID: UUID) {
        val token = getOAuthAccessToken(
            clientId = tokenConfig.clientId,
            url = "${workLoad.gatling.tokenEndpointHost}/${tokenConfig.endpoint}",
            username = tokenConfig.username,
            password = tokenConfig.password,
        )
        runBlocking {
            DockerExecutor().executeDocker(
                "docker run -d " +
                        "--network infrastructure-docker_default " +
                        "-e TEST_CLASS=org.misarch.${workLoad.gatling.loadType} " +
                        "-e ACCESS_TOKEN=$token " +
                        "-e BASE_URL=${workLoad.gatling.endpointHost} " +
                        "-e TEST_UUID=${testUUID} " +
                        "-e EXPERIMENT_EXECUTOR_URL=$experimentExecutorHost " +
                        "-v ${workLoad.gatling.userStepsPathUri}:/gatling/src/main/resources/$GATLING_USERSTEPS_FILENAME " +
                        "-v ${workLoad.gatling.workPathUri}:/gatling/src/main/kotlin/scenarios/$GATLING_WORK_FILENAME " +
                        "gatling-test gradle gatlingRun forwardMetrics"
            )
        }
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