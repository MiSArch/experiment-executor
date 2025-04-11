package org.misarch.experimentexecutor.plugin.failure.misarch

import kotlinx.coroutines.delay
import org.misarch.experimentexecutor.executor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

class MisarchExperimentConfigPlugin(private val webClient: WebClient) : FailurePluginInterface {
    override suspend fun executeFailure(failure: Failure): Boolean {
        runCatching { configureVariable() }.getOrElse { return false }
        return true
    }
    
    private suspend fun configureVariable() {
        println("Configuring serviceInvocationDeterioration variable for catalog as null")
        webClient.put()
            .uri("http://localhost:3000/configuration/catalog/variables/serviceInvocationDeterioration")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("value" to null))
            .retrieve()
            .awaitBody<String>()

        delay(15000)

        println("Configuring serviceInvocationDeterioration variable for catalog as delay 1000")
        webClient.put()
            .uri("http://localhost:3000/configuration/catalog/variables/serviceInvocationDeterioration")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .bodyValue(
                mapOf(
                    "value" to listOf(
                        mapOf(
                            "path" to "/",
                            "delay" to 1000,
                            "delayProbability" to 1,
                            "errorProbability" to 0,
                            "errorCode" to 404
                        )
                    )
                )
            )
            .retrieve()
            .awaitBody<String>()
    }
}