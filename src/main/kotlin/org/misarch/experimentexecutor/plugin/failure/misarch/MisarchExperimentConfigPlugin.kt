package org.misarch.experimentexecutor.plugin.failure.misarch

import ArtificialCPUUsage
import MiSArchFailureConfig
import PubSubDeterioration
import ServiceInvocationDeterioration
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.misarch.experimentexecutor.config.MISARCH_EXPERIMENT_CONFIG_FILENAME
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class MisarchExperimentConfigPlugin(
    private val webClient: WebClient,
    private val misarchExperimentConfigHost: String,
    private val basePath: String,
) : FailurePluginInterface {
    private val configMap = ConcurrentHashMap<String, List<MiSArchFailureConfig>>()
    private val stoppableJobs = ConcurrentHashMap<String, Job>()

    override suspend fun initializeFailure(
        testUUID: UUID,
        testVersion: String,
    ) {
        val testId = "$testUUID:$testVersion"
        configMap[testId] = readConfigFile("$basePath/$testUUID/$testVersion/$MISARCH_EXPERIMENT_CONFIG_FILENAME")
    }

    override suspend fun startTimedExperiment(
        testUUID: UUID,
        testVersion: String,
    ) {
        val testId = "$testUUID:$testVersion"
        // TODO somehow there are sometimes key-errors here
        if (configMap.containsKey(testId)) {
            coroutineScope {
                stoppableJobs[testId] =
                    launch {
                        val config = configMap.getValue(testId)
                        configMap.remove(testId)
                        configureVariables(config)
                    }
            }
        }
    }

    override suspend fun stopExperiment(
        testUUID: UUID,
        testVersion: String,
    ) {
        val testId = "$testUUID:$testVersion"
        if (!stoppableJobs.containsKey(testId) && configMap.containsKey(testId)) {
            configMap.remove(testId)
        } else {
            stoppableJobs[testId]?.cancel()
            stoppableJobs.remove(testId)
        }
        logger.info { "Stopped Misarch Experiment Configuration for testUUID: $testUUID and testVersion: $testVersion" }
    }

    private fun readConfigFile(pathUri: String): List<MiSArchFailureConfig> {
        val file = File(pathUri)
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
            bodyValue = mapOf("value" to pubSubDeterioration),
        )
    }

    private suspend fun configureServiceInvocationDeterioration(
        component: String,
        serviceInvocationDeteriorations: List<ServiceInvocationDeterioration>?,
    ) {
        configureVariable(
            component = component,
            variable = "serviceInvocationDeterioration",
            bodyValue = mapOf("value" to serviceInvocationDeteriorations),
        )
    }

    private suspend fun configureArtificialMemoryUsage(
        component: String,
        artificialMemoryUsage: Long?,
    ) {
        configureVariable(
            component = component,
            variable = "artificialMemoryUsage",
            bodyValue = mapOf("value" to artificialMemoryUsage),
        )
    }

    private suspend fun configureArtificialCPUUsage(
        component: String,
        artificialCPUUsage: List<ArtificialCPUUsage>?,
    ) {
        configureVariable(
            component = component,
            variable = "artificialCPUUsage",
            bodyValue = mapOf("value" to artificialCPUUsage),
        )
    }

    private suspend fun configureVariable(
        component: String,
        variable: String,
        bodyValue: Any,
    ) {
        val response =
            webClient
                .put()
                .uri("$misarchExperimentConfigHost/configuration/$component/variables/$variable")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyValue)
                .retrieve()
                .awaitBody<String>()
        logger.info { "Configured Variable: $response" }
    }
}
