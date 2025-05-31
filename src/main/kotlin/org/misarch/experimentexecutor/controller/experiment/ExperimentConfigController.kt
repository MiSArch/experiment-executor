package org.misarch.experimentexecutor.controller.experiment

import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.model.GatlingLoadType
import org.misarch.experimentexecutor.service.ExperimentConfigService
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class ExperimentConfigController(
    private val experimentConfigService: ExperimentConfigService,
) {
    /**
     * Generates and stores a new experiment configuration based on the specified load type.
     */
    @PostMapping("/experiment/generate/{loadType}")
    suspend fun generateExperiment(
        @PathVariable loadType: GatlingLoadType,
        @RequestParam(required = false, defaultValue = "10") sessionDuration: Int,
        @RequestParam(required = false, defaultValue = "1800") testDuration: Int,
        @RequestParam(required = false) rate: Float?,
    ): String {
        val defaultRate = rate ?: (2.0F / sessionDuration)
        return experimentConfigService.generateExperiment(loadType, testDuration, sessionDuration, defaultRate)
    }

    @PostMapping("/experiment/{testUUID}/{testVersion}/newVersion")
    suspend fun newExperimentVersion(@PathVariable testUUID: UUID, @PathVariable testVersion: String): String {
        return experimentConfigService.newExperimentVersion(testUUID, testVersion)
    }

    @GetMapping("/experiment/{testUUID}/{testVersion}/chaosToolkitConfig")
    suspend fun getChaosToolkit(@PathVariable testUUID: UUID, @PathVariable testVersion: String): String {
        return experimentConfigService.getChaosToolkitConfig(testUUID, testVersion)
    }

    @PutMapping("/experiment/{testUUID}/{testVersion}/chaosToolkitConfig")
    suspend fun putChaosToolkit(@PathVariable testUUID: UUID, @PathVariable testVersion: String, @RequestBody chaosToolKitConfig: String) {
        return experimentConfigService.updateChaosToolkitConfig(testUUID, testVersion, chaosToolKitConfig)
    }

    @GetMapping("/experiment/{testUUID}/{testVersion}/misarchExperimentConfig")
    suspend fun getMisarchExperimentConfig(@PathVariable testUUID: UUID, @PathVariable testVersion: String): String {
        return experimentConfigService.getMisarchExperimentConfig(testUUID, testVersion)
    }

    @PutMapping("/experiment/{testUUID}/{testVersion}/misarchExperimentConfig")
    suspend fun putMisarchExperimentConfig(@PathVariable testUUID: UUID, @PathVariable testVersion: String, @RequestBody misarchExperimentConfig: String) {
        return experimentConfigService.updateMisarchExperimentConfig(testUUID, testVersion, misarchExperimentConfig)
    }

    @GetMapping("/experiment/{testUUID}/{testVersion}/gatlingConfig/userSteps")
    suspend fun getGatlingUserSteps(@PathVariable testUUID: UUID, @PathVariable testVersion: String): String {
        return experimentConfigService.getGatlingUsersteps(testUUID, testVersion)
    }

    @PutMapping("/experiment/{testUUID}/{testVersion}/gatlingConfig/userSteps")
    suspend fun putGatlingUsersteps(@PathVariable testUUID: UUID, @PathVariable testVersion: String, @RequestBody userSteps: String) {
        return experimentConfigService.updateGatlingUsersteps(testUUID, testVersion, userSteps)
    }

    @GetMapping("/experiment/{testUUID}/{testVersion}/gatlingConfig/work")
    suspend fun getGatlingWork(@PathVariable testUUID: UUID, @PathVariable testVersion: String): String {
        return experimentConfigService.getGatlingWork(testUUID, testVersion)
    }

    @PutMapping("/experiment/{testUUID}/{testVersion}/gatlingConfig/work")
    suspend fun putGatlingWork(@PathVariable testUUID: UUID, @PathVariable testVersion: String, @RequestBody work: String) {
        return experimentConfigService.updateGatlingWork(testUUID, testVersion, work)
    }

    @GetMapping("/experiment/{testUUID}/{testVersion}/config")
    suspend fun getExperimentConfig(@PathVariable testUUID: UUID, @PathVariable testVersion: String): ExperimentConfig {
        return experimentConfigService.getExperimentConfig(testUUID, testVersion)
    }

    @PutMapping("/experiment/{testUUID}/{testVersion}/config")
    suspend fun putExperimentConfig(@PathVariable testUUID: UUID, @PathVariable testVersion: String, @RequestBody experimentConfig: ExperimentConfig) {
        return experimentConfigService.updateExperimentConfig(testUUID, testVersion, experimentConfig)
    }
}