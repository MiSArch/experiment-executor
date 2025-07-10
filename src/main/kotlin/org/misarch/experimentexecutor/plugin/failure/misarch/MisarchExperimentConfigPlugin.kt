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
import kotlinx.coroutines.withTimeout
import org.misarch.experimentexecutor.config.MISARCH_EXPERIMENT_CONFIG_FILENAME
import org.misarch.experimentexecutor.config.MISARCH_SERVICES
import org.misarch.experimentexecutor.plugin.failure.FailurePluginInterface
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.reactive.function.client.awaitBody
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class MisarchExperimentConfigPlugin(
    private val webClient: WebClient,
    private val misarchExperimentConfigHost: String,
    private val experimentExecutorUrl: String,
    private val basePath: String,
) : FailurePluginInterface {
    private val stoppableJobs = ConcurrentHashMap<String, Job>()

    override suspend fun initializeFailure(
        testUUID: UUID,
        testVersion: String,
        testDelay: Int,
    ) {
        coroutineScope {
            val testId = "$testUUID:$testVersion"
            stoppableJobs[testId] =
                launch {
                    val config = readConfigFile("$basePath/$testUUID/$testVersion/$MISARCH_EXPERIMENT_CONFIG_FILENAME")
                    initiallyResetConfiguration()
                    registerPlugin(testUUID, testVersion)
                    var triggerState = checkTriggerState(testUUID, testVersion)
                    0.until(6000 + testDelay).forEach { _ ->
                        if (triggerState) {
                            return@forEach
                        }
                        delay(100)
                        triggerState = checkTriggerState(testUUID, testVersion)
                    }
                    configureVariables(config)
                }
        }
    }

    override suspend fun stopExperiment(
        testUUID: UUID,
        testVersion: String,
    ) {
        val testId = "$testUUID:$testVersion"
        if (!stoppableJobs.containsKey(testId)) return

        stoppableJobs[testId]?.cancel()
        stoppableJobs.remove(testId)

        logger.info { "Stopped Misarch Experiment Configuration for testUUID: $testUUID and testVersion: $testVersion" }
    }

    private fun readConfigFile(pathUri: String): List<MiSArchFailureConfig> {
        val file = File(pathUri)
        val objectMapper = ObjectMapper()
        return objectMapper.readValue(file, object : TypeReference<List<MiSArchFailureConfig>>() {})
    }

    private suspend fun configureVariables(misarchFailures: List<MiSArchFailureConfig>) {
        misarchFailures.forEachIndexed { i, f ->
            delay(f.pauses.before * 1000L)
            logger.info { "Configure MiSArch Experiment Configuration for failure set ${i + 1}." }
            f.failures.forEach { failure ->
                configurePubSubDeterioration(failure.name, failure.pubSubDeterioration)
                configureServiceInvocationDeterioration(failure.name, failure.serviceInvocationDeterioration)
                configureArtificialMemoryUsage(failure.name, failure.artificialMemoryUsage)
                configureArtificialCPUUsage(failure.name, failure.artificialCPUUsage)
            }
            delay(f.pauses.after * 1000L)
        }
    }

    private suspend fun initiallyResetConfiguration() {
        MISARCH_SERVICES.forEach { service ->
            configurePubSubDeterioration(service, null)
            configureServiceInvocationDeterioration(service, null)
            configureArtificialMemoryUsage(service, null)
            configureArtificialCPUUsage(service, null)
        }

        logger.info { "Reset MiSArch Experiment Configuration for all services" }
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
        withRetries {
            withTimeout(1500) {
                webClient
                    .put()
                    .uri("$misarchExperimentConfigHost/configuration/$component/variables/$variable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bodyValue)
                    .retrieve()
                    .awaitBody<String>()
            }
        }
    }

    private suspend fun registerPlugin(
        testUUID: UUID,
        testVersion: String,
    ) {
        webClient
            .post()
            .uri("$experimentExecutorUrl/trigger/$testUUID/$testVersion?client=misarchExperimentConfig")
            .retrieve()
            .awaitBodilessEntity()
    }

    private suspend fun checkTriggerState(
        testUUID: UUID,
        testVersion: String,
    ): Boolean =
        webClient
            .get()
            .uri("$experimentExecutorUrl/trigger/$testUUID/$testVersion")
            .retrieve()
            .awaitBody<String>()
            .toBoolean()
}
