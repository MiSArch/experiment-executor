package org.misarch.experimentexecutor.controller.experiment

import org.misarch.experimentexecutor.config.CORS_ORIGIN
import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.service.ExperimentExecutionService
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@CrossOrigin(origins = [CORS_ORIGIN])
class ExperimentExecutionController(
    private val experimentExecutionService: ExperimentExecutionService,
) {
    /**
     * Runs an experiment with the provided configuration file.
     */
    @PostMapping("/experiment")
    suspend fun runExperimentWithConfigFile(@RequestBody experimentConfig: ExperimentConfig): String {
        return experimentExecutionService.executeExperiment(experimentConfig, UUID.fromString(experimentConfig.testUUID))
    }

    /**
     * Runs an experiment based on a stored test configuration identified by its UUID.
     */
    @PostMapping("/experiment/{testUUID}")
    suspend fun runExperiment(@PathVariable testUUID: UUID): String {
        return experimentExecutionService.executeStoredExperiment(testUUID)
    }

    /**
     * Returns the current state of the trigger for the specified test UUID.
     * Used for synchronizing all plugins that are waiting for the trigger to be ready.
     */
    @GetMapping("/trigger/{testUUID}")
    suspend fun trigger(@PathVariable testUUID: UUID): String {
        return experimentExecutionService.getTriggerState(testUUID).toString()
    }



}