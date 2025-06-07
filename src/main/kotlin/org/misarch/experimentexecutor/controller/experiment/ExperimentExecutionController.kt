package org.misarch.experimentexecutor.controller.experiment

import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.service.ExperimentExecutionService
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class ExperimentExecutionController(
    private val experimentExecutionService: ExperimentExecutionService,
) {
    /**
     * Runs an experiment with the provided configuration file.
     */
    @PostMapping("/experiment")
    private suspend fun runExperimentWithConfigFile(
        @RequestBody experimentConfig: ExperimentConfig,
    ) = experimentExecutionService.executeExperiment(
        experimentConfig,
        UUID.fromString(experimentConfig.testUUID),
        experimentConfig.testVersion,
    )

    /**
     * Runs an experiment based on a stored test configuration identified by its UUID.
     */
    @PostMapping("/experiment/{testUUID}/{testVersion}")
    private suspend fun runExperiment(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
        @RequestParam endpointAccessToken: String? = null,
    ) = experimentExecutionService.executeStoredExperiment(testUUID, testVersion, endpointAccessToken)

    /**
     * Stops the currently running experiment identified by its UUID.
     * This will stop the workload and the failure plugins.
     */
    @DeleteMapping("/experiment/{testUUID}/{testVersion}")
    private suspend fun stopExperiment(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
    ) {
        experimentExecutionService.cancelExperiment(testUUID, testVersion)
    }

    /**
     * Returns the current state of the trigger for the specified test UUID.
     * Used for synchronizing all plugins that are waiting for the trigger to be ready.
     */
    @GetMapping("/trigger/{testUUID}/{testVersion}")
    private suspend fun trigger(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
    ): String = experimentExecutionService.getTriggerState(testUUID, testVersion).toString()

    /**
     * Collects Gatling metrics from Gatling's index.html and stats.js files transferred as concatenated plaintext strings.
     */
    @PostMapping("/experiment/{testUUID}/{testVersion}/gatling/metrics")
    private suspend fun collectGatingMetrics(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
        @RequestBody data: String,
    ) {
        val test = data.split("\nSPLIT_HERE\n")
        val rawHtml = test[0]
        val rawJs = test[1]
        experimentExecutionService.finishExperiment(testUUID, testVersion, rawJs, rawHtml)
    }
}
