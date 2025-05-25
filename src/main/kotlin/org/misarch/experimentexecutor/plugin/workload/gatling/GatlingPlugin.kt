package org.misarch.experimentexecutor.plugin.workload.gatling

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.misarch.experimentexecutor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class GatlingPlugin(private val webClient: WebClient) : WorkloadPluginInterface {
    private var containerId: String? = null

    override suspend fun executeWorkLoad(workLoad: WorkLoad, testUUID: UUID): Boolean {
        withContext(Dispatchers.IO) {
            val token = getOAuthAccessToken(
                clientId = "frontend",
                url = "${workLoad.gatling!!.tokenEndpointHost}/keycloak/realms/Misarch/protocol/openid-connect/token",
                username = "eliasmueller",
                password = "123",
            )
            val process = ProcessBuilder(
                "bash", "-c",
                "docker run -d " +
                        "--network infrastructure-docker_default " +
                        "-e TEST_CLASS=org.misarch.${workLoad.gatling.loadType} " +
                        "-e ACCESS_TOKEN=$token " +
                        "-e BASE_URL=${workLoad.gatling.endpointHost} " +
                        "-e TEST_UUID=${testUUID} " +
                        "-e EXPERIMENT_EXECUTOR_URL=http://192.168.178.155:8888 " +
                        "-v ${workLoad.gatling.pathUri}/gatling-usersteps.csv:/gatling/src/main/resources/gatling-usersteps.csv " +
                        "-v ${workLoad.gatling.pathUri}/GatlingScenario.kt:/gatling/src/main/kotlin/scenarios/GatlingScenario.kt " +
                        "gatling-test gradle gatlingRun forwardMetrics"
            ).redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                containerId = reader.readLine()?.trim()
            }

            if (containerId != null) {
                ProcessBuilder("bash", "-c", "docker wait $containerId").start().waitFor()
                ProcessBuilder("bash", "-c", "docker rm $containerId").start().waitFor()
            }

        }
        return containerId != null
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