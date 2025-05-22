package org.misarch.experimentexecutor.service.experiment

/*import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.reactor.awaitSingle
import org.misarch.experimentexecutor.executor.model.*
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.io.File


@Service
class K6LoadTestGenerator(
    private val graphQLQueryGeneratorService: GraphQLQueryGeneratorService,
    private val webClient: WebClient,
) {

    suspend fun generateLoadTest(workLoad: WorkLoad): String {
        if (workLoad.graphql != null) {
            val graphQLQueriesAndMutations = graphQLQueryGeneratorService.generateGraphQL(workLoad.graphql.uri)
            workLoad.graphql.flows.forEach { flow ->

                val orderedRequests = mutableListOf<Request>()
                val k6Requests = mutableListOf<String>()

                flow.requests.forEach { request ->
                    request.orderRequestsByInputs(flow, orderedRequests)
                }
                orderedRequests.forEach { request ->
                    // TODO how to handle requests without inputs?
                    // TODO implement oauth flow
                    val inputVariable = request.inputs?.first()?.let { input ->
                        if (input.refInput != null && input.refInput.list?.isNotEmpty() == true) {
                            "JSON.parse(${input.refInput.name}_res.body).data.${input.refInput.name}.${input.refInput.list.first().first}[0].${(input.refInput).list.first().second}"
                        } else null
                    }
                    val queryOrMutation = if (inputVariable != null) {
                        graphQLQueriesAndMutations.getValue(request.name).replace("UUID", "${"$"}{$inputVariable}")
                    } else {
                        graphQLQueriesAndMutations.getValue(request.name)
                    }
                    val k6Request = """
                        const ${request.name} = `${queryOrMutation}`
                        const ${request.name}_payload = JSON.stringify({query: ${request.name}});
                        const ${request.name}_params = {
                            headers: {
                                'Content-Type': 'application/json',
                                'Authorization': `Bearer ${"$"}{BEARER_TOKEN}`
                            }
                        };
                        const ${request.name}_res = http.post(GRAPHQL_ENDPOINT, ${request.name}_payload, ${request.name}_params);
                    """.trimIndent()
                    k6Requests.add(k6Request)
                }
                File("k6Script.js").writeText(generateK6Script(k6Requests, workLoad))
            }

        }

        return "TODO this will be the uri of the load test"
    }

    private suspend fun Request.orderRequestsByInputs(flow: Flow, orderedRequests: MutableList<Request>) {
        inputs?.forEach { input ->
            if (input.refInput != null) {
                val predecessingRequest =
                    flow.requests.first { input.refInput == it.outputs?.first { output -> output.name == input.refInput.name } }
                predecessingRequest.orderRequestsByInputs(flow, orderedRequests)
            }
        }
        if (orderedRequests.contains(this)) {
            orderedRequests.remove(this)
        }
        orderedRequests.add(this)
    }

    private suspend fun generateK6Script(queries: List<String>, workLoad: WorkLoad): String {
        val token = getOAuthAccessToken(
            clientId = "frontend",
            url = "http://localhost:8081/keycloak/realms/Misarch/protocol/openid-connect/token",
            username = "elilasmueller",
            password = "123",
        )
        return """
            import http from 'k6/http';
            import { check, sleep } from 'k6';

            const GRAPHQL_ENDPOINT = '${workLoad.graphql?.uri}';
            const BEARER_TOKEN = '$token';

            export let options = {
              vus: 10,
              duration: '30s',
            };

            export default function () {
                ${queries.joinToString("\n\n")}
                sleep(1);
            }
        """.trimIndent()
    }

    private suspend fun getOAuthAccessToken(clientId: String, url: String, username: String, password: String): String {
        val data = mapOf(
            "grant_type" to "password",
            "client_id" to clientId,
            "username" to username,
            "password" to password
        )
        val response = webClient.post()
            .uri(url)
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
)*/