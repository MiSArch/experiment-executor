package org.misarch.experimentexecutor.plugin.failure.misarch

import ArtificialCPUUsage
import MiSArchFailureConfig
import PubSubDeterioration
import ServiceInvocationDeterioration
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.delay
import org.misarch.experimentexecutor.model.Failure
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.io.File
import java.util.UUID

class MisarchExperimentConfigPlugin(private val webClient: WebClient) : FailurePluginInterface {
    private var config: List<MiSArchFailureConfig> = emptyList()

    override suspend fun executeFailure(failure: Failure, testUUID: UUID): Boolean {
        return runCatching {
            config = readConfigFile(failure.experimentConfig!!.pathUri)
            true
        }.getOrElse {
            false
        }
    }

    override suspend fun startExperiment(): Boolean {
        return runCatching {
            configureVariables(config)
            true
        }.getOrElse {
            false
        }
    }

    private fun readConfigFile(pathUri: String): List<MiSArchFailureConfig> {
        val file = File(pathUri)
        if (!file.exists()) {
            throw IllegalArgumentException("Configuration file not found at $pathUri")
        }

        val objectMapper = ObjectMapper()
        return objectMapper.readValue(file, object : TypeReference<List<MiSArchFailureConfig>>() {})
    }

    private suspend fun configureVariables(misarchFailures: List<MiSArchFailureConfig>) {
        misarchFailures.forEach { f ->
            f.failures.forEach { failure ->
                configurePubSubDeterioration(failure.name, failure.pubSubDeterioration)
                configureServiceInvocationDeterioration(failure.name, failure.serviceInvocationDeterioration)
                configureArtificialMemoryUsage(failure.name, failure.artificialMemoryUsage)
                configureArtificialCPUUsage(failure.name, failure.artificialCPUUsage)
            }
            delay(f.pause)
        }
    }

    private suspend fun configurePubSubDeterioration(
        component: String,
        pubSubDeterioration: PubSubDeterioration?,
    ) {
        configureVariable(
            component = component,
            variable = "pubsubDeterioration",
            bodyValue = mapOf("value" to pubSubDeterioration)
        )
    }

    private suspend fun configureServiceInvocationDeterioration(
        component: String,
        serviceInvocationDeteriorations: List<ServiceInvocationDeterioration>?,
    ) {
        configureVariable(
            component = component,
            variable = "serviceInvocationDeterioration",
            bodyValue = mapOf("value" to serviceInvocationDeteriorations)
        )
    }

    private suspend fun configureArtificialMemoryUsage(
        component: String,
        artificialMemoryUsage: Long?,
    ) {
        configureVariable(
            component = component,
            variable = "artificialMemoryUsage",
            bodyValue = mapOf("value" to artificialMemoryUsage)
        )
    }

    private suspend fun configureArtificialCPUUsage(
        component: String,
        artificialCPUUsage: List<ArtificialCPUUsage>?,
    ) {
        configureVariable(
            component = component,
            variable = "artificialCPUUsage",
            bodyValue = mapOf("value" to artificialCPUUsage)
        )
    }

    private suspend fun configureVariable(
        component: String,
        variable: String,
        bodyValue: Any,
    ) {
        val response = webClient.put()
            .uri("http://localhost:3000/configuration/$component/variables/$variable")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(bodyValue)
            .retrieve()
            .awaitBody<String>()
        println("Configured Variable: $response")
    }
}