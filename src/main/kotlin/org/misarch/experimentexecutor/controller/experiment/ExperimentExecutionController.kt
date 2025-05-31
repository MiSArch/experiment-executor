package org.misarch.experimentexecutor.controller.experiment

import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.service.ExperimentExecutionService
import org.springframework.web.bind.annotation.*
import java.util.*

// TODO implement server side events to handle the experiment execution state and notify the frontend once an experiment is finished
@RestController
class ExperimentExecutionController(
    private val experimentExecutionService: ExperimentExecutionService,
) {
    /**
     * Runs an experiment with the provided configuration file.
     */
    @PostMapping("/experiment")
    suspend fun runExperimentWithConfigFile(@RequestBody experimentConfig: ExperimentConfig): String {
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
    suspend fun runExperiment(@PathVariable testUUID: UUID, @PathVariable testVersion: String, @RequestBody endpointAccessToken: String? = null):
            String {
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
    suspend fun trigger(@PathVariable testUUID: UUID, @PathVariable testVersion: String): String {
        return experimentExecutionService.getTriggerState(testUUID, testVersion).toString()
    }

    /**
     * Collects Gatling metrics from Gatling's index.html and stats.js files transferred as concatenated plaintext strings.
     */
    @PostMapping("/experiment/{testUUID}/{testVersion}/gatling/metrics")
    private suspend fun collectGatingMetrics(@PathVariable testUUID: UUID, @PathVariable testVersion: String, @RequestBody data: String) {
        val test = data.split("\nSPLIT_HERE\n")
        val rawHtml = test[0]
        val rawJs = test[1]
        experimentExecutionService.finishExperiment(testUUID, testVersion, rawJs, rawHtml)
    }
}