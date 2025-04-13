package org.misarch.experimentexecutor.plugin.workload.gatling

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.reactor.awaitSingle
import org.misarch.experimentexecutor.executor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.workload.WorkloadPluginInterface
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.io.File

class GatlingPlugin(private val webClient: WebClient) : WorkloadPluginInterface {
    override suspend fun executeWorkLoad(workLoad: WorkLoad): Boolean {
        val token = getOAuthAccessToken(
            clientId = "frontend",
            url = "http://localhost:8081/keycloak/realms/Misarch/protocol/openid-connect/token",
            username = "eliasmueller",
            password = "123",
        )
        return runCatching {
            val process = ProcessBuilder("./gradlew", "gatlingRun")
                .directory(File("/Users/p371728/master/thesis/misarch/gatling-test/untitled"))
                .redirectOutput(ProcessBuilder.Redirect.INHERIT) // Redirect output to console
                .redirectError(ProcessBuilder.Redirect.INHERIT) // Redirect error to console
                .apply {
                    environment()["ACCESS_TOKEN"] = token
                }
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