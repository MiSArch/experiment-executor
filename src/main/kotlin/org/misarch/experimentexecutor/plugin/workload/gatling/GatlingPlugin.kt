package org.misarch.experimentexecutor.plugin.workload.gatling

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.reactor.awaitSingle
import org.misarch.experimentexecutor.executor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

class GatlingPlugin(private val webClient: WebClient) : WorkloadPluginInterface {
    override suspend fun executeWorkLoad(workLoad: WorkLoad, testUUID: UUID): Boolean {
        val token = getOAuthAccessToken(
            clientId = "frontend",
            url = "${workLoad.gatling!!.tokenEndpointHost}/keycloak/realms/Misarch/protocol/openid-connect/token",
            username = "eliasmueller",
            password = "123",
        )
        return runCatching {
            val process = ProcessBuilder(
                "bash", "-c",
                "docker run -d " +
                        "--network infrastructure-docker_default " +
                        "-e TEST_CLASS=org.misarch.${workLoad.gatling.loadType} " +
                        "-e ACCESS_TOKEN=$token " +
                        "-e BASE_URL=${workLoad.gatling.endpointHost} " +
                        "-e TEST_UUID=${testUUID} " +
                        "-v ${workLoad.gatling.pathUri}/gatling-usersteps.csv:/gatling/src/main/resources/gatling-usersteps.csv " +
                        "gatling-test gradle gatlingRun forwardMetrics"
            )
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            val exitCode = process.waitFor()
            exitCode == 0
        }.getOrElse { e ->
            e.printStackTrace()
            false
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