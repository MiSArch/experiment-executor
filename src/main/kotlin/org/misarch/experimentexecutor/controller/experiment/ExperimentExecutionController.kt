package org.misarch.experimentexecutor.controller.experiment

import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.service.ExperimentExecutionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Configuration
class EventEmitterConfig {

    @Bean
    fun eventEmitters(): ConcurrentHashMap<String, FluxSink<String>> {
        return ConcurrentHashMap()
    }
}

@RestController
class ExperimentExecutionController(
    private val experimentExecutionService: ExperimentExecutionService,
    private val eventEmitters: ConcurrentHashMap<String, FluxSink<String>>
) {
    @GetMapping("/experiment/{testUUID}/{testVersion}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    private fun registerEvent(@PathVariable testUUID: UUID, @PathVariable testVersion: String): Flux<String> {
        val key = "$testUUID:$testVersion"
        return Flux.create { sink ->
            eventEmitters[key] = sink
            sink.onDispose { eventEmitters.remove(key) }
        }
    }

    /**
     * Runs an experiment with the provided configuration file.
     */
    @PostMapping("/experiment")
    private suspend fun runExperimentWithConfigFile(@RequestBody experimentConfig: ExperimentConfig) {
        return experimentExecutionService.executeExperiment(
            experimentConfig,
            UUID.fromString(experimentConfig.testUUID),
            experimentConfig.testVersion
        )
    }

    /**
     * Runs an experiment based on a stored test configuration identified by its UUID.
     */
    @PostMapping("/experiment/{testUUID}/{testVersion}")
    private suspend fun runExperiment(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
        @RequestParam endpointAccessToken: String? = null
    ) {
        return experimentExecutionService.executeStoredExperiment(testUUID, testVersion, endpointAccessToken)
    }

    /**
     * Stops the currently running experiment identified by its UUID.
     * This will stop the workload and the failure plugins.
     */
    @DeleteMapping("/experiment/{testUUID}/{testVersion}")
    private suspend fun stopExperiment(@PathVariable testUUID: UUID, @PathVariable testVersion: String) {
        experimentExecutionService.cancelExperiment(testUUID, testVersion)
    }

    /**
     * Returns the current state of the trigger for the specified test UUID.
     * Used for synchronizing all plugins that are waiting for the trigger to be ready.
     */
    @GetMapping("/trigger/{testUUID}/{testVersion}")
    private suspend fun trigger(@PathVariable testUUID: UUID, @PathVariable testVersion: String): String {
        return experimentExecutionService.getTriggerState(testUUID, testVersion).toString()
    }

    /**
     * Collects Gatling metrics from Gatling's index.html and stats.js files transferred as concatenated plaintext strings.
     */
    @PostMapping("/experiment/{testUUID}/{testVersion}/gatling/metrics")
    private suspend fun collectGatingMetrics(@PathVariable testUUID: UUID, @PathVariable testVersion: String, @RequestBody data: String) {
        val key = "$testUUID:$testVersion"
        val test = data.split("\nSPLIT_HERE\n")
        val rawHtml = test[0]
        val rawJs = test[1]
        experimentExecutionService.finishExperiment(testUUID, testVersion, rawJs, rawHtml)

        // TODO do not emit here, but only when no error occurred in async part
        val eventSink = eventEmitters[key]
        eventSink?.next("http://localhost:3001/d/$testUUID-$testVersion")
        eventEmitters.remove(key)
    }
}