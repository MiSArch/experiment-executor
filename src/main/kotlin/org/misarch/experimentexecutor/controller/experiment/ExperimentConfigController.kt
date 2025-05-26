package org.misarch.experimentexecutor.controller.experiment

import org.misarch.experimentexecutor.config.CORS_ORIGIN
import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.model.GatlingLoadType
import org.misarch.experimentexecutor.service.ExperimentConfigService
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@CrossOrigin(origins = [CORS_ORIGIN])
class ExperimentConfigController(
    private val experimentConfigService: ExperimentConfigService,
) {
    /**
     * Generates and stores a new experiment configuration based on the specified load type.
     */
    @PostMapping("/experiment/generate/{loadType}")
    suspend fun generateExperiment(@PathVariable loadType: GatlingLoadType): String {
        return experimentConfigService.generateExperiment(loadType)
    }

    @GetMapping("/experiment/{testUUID}/chaosToolkitConfig")
    suspend fun getChaosToolkit(@PathVariable testUUID: UUID): String {
        return experimentConfigService.getChaosToolkitConfig(testUUID)
    }

    @PutMapping("/experiment/{testUUID}/chaosToolkitConfig")
    suspend fun putChaosToolkit(@PathVariable testUUID: UUID, @RequestBody chaosToolKitConfig: String) {
        return experimentConfigService.updateChaosToolkitConfig(testUUID, chaosToolKitConfig)
    }

    @GetMapping("/experiment/{testUUID}/misarchExperimentConfig")
    suspend fun getMisarchExperimentConfig(@PathVariable testUUID: UUID): String {
        return experimentConfigService.getMisarchExperimentConfig(testUUID)
    }

    @PutMapping("/experiment/{testUUID}/misarchExperimentConfig")
    suspend fun putMisarchExperimentConfig(@PathVariable testUUID: UUID, @RequestBody misarchExperimentConfig: String) {
        return experimentConfigService.updateMisarchExperimentConfig(testUUID, misarchExperimentConfig)
    }

    @GetMapping("/experiment/{testUUID}/gatlingConfig/userSteps")
    suspend fun getGatlingUserSteps(@PathVariable testUUID: UUID): String {
        return experimentConfigService.getGatlingUsersteps(testUUID)
    }

    @PutMapping("/experiment/{testUUID}/gatlingConfig/userSteps")
    suspend fun putGatlingUsersteps(@PathVariable testUUID: UUID, @RequestBody userSteps: String) {
        return experimentConfigService.updateGatlingUsersteps(testUUID, userSteps)
    }

    @GetMapping("/experiment/{testUUID}/gatlingConfig/work")
    suspend fun getGatlingWork(@PathVariable testUUID: UUID): String {
        return experimentConfigService.getGatlingWork(testUUID)
    }

    @PutMapping("/experiment/{testUUID}/gatlingConfig/work")
    suspend fun putGatlingWork(@PathVariable testUUID: UUID, @RequestBody work: String) {
        return experimentConfigService.updateGatlingWork(testUUID, work)
    }

    @GetMapping("/experiment/{testUUID}/config")
    suspend fun getExperimentConfig(@PathVariable testUUID: UUID): ExperimentConfig {
        return experimentConfigService.getExperimentConfig(testUUID)
    }

    @PutMapping("/experiment/{testUUID}/config")
    suspend fun putExperimentConfig(@PathVariable testUUID: UUID, @RequestBody experimentConfig: ExperimentConfig) {
        return experimentConfigService.updateExperimentConfig(testUUID, experimentConfig)
    }
}