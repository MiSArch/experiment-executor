package org.misarch.experimentexecutor.controller.experiment

import org.misarch.experimentexecutor.controller.experiment.model.EncodedFileDTO
import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.model.GatlingLoadType
import org.misarch.experimentexecutor.service.ExperimentConfigService
import org.misarch.experimentexecutor.service.ExperimentGeneratorService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.HtmlUtils
import java.util.UUID

@RestController
class ExperimentConfigController(
    private val experimentConfigService: ExperimentConfigService,
    private val experimentGeneratorService: ExperimentGeneratorService,
) {
    /**
     * Generates and stores a new experiment configuration based on the specified load type.
     */
    @PostMapping("/experiment/generate")
    suspend fun generateExperiment(
        @RequestParam loadType: GatlingLoadType,
        @RequestParam(required = false, defaultValue = "My Experiment") testName: String,
        @RequestParam(required = false, defaultValue = "10") maximumArrivingUsersPerSecond: Int,
        @RequestParam(required = false, defaultValue = "1800") testDuration: Int,
        @RequestParam(required = false) rate: Float?,
        @RequestBody(required = false) sessionDurations: List<Int>,
    ): String {
        val sanitizedTestName = HtmlUtils.htmlEscape(testName)
        val defaultRate = rate ?: 1F
        return experimentGeneratorService.generateExperiment(
            sanitizedTestName,
            loadType,
            testDuration,
            maximumArrivingUsersPerSecond,
            sessionDurations,
            defaultRate,
        )
    }

    @GetMapping("/experiments")
    suspend fun getExistingExperiments(): List<String> = experimentConfigService.getExistingExperiments()

    @GetMapping("/experiment/{testUUID}/versions")
    suspend fun getExperimentVersions(
        @PathVariable testUUID: UUID,
    ): List<String> = experimentConfigService.getExperimentVersions(testUUID)

    @PostMapping("/experiment/{testUUID}/{testVersion}/newVersion")
    suspend fun newExperimentVersion(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
    ): String = experimentConfigService.newExperimentVersion(testUUID, testVersion)

    @GetMapping("/experiment/{testUUID}/{testVersion}/chaosToolkitConfig")
    suspend fun getChaosToolkit(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
    ): String = experimentConfigService.getChaosToolkitConfig(testUUID, testVersion)

    @PutMapping("/experiment/{testUUID}/{testVersion}/chaosToolkitConfig")
    suspend fun putChaosToolkit(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
        @RequestBody chaosToolKitConfig: String,
    ) = experimentConfigService.updateChaosToolkitConfig(testUUID, testVersion, chaosToolKitConfig)

    @GetMapping("/experiment/{testUUID}/{testVersion}/misarchExperimentConfig")
    suspend fun getMisarchExperimentConfig(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
    ): String = experimentConfigService.getMisarchExperimentConfig(testUUID, testVersion)

    @PutMapping("/experiment/{testUUID}/{testVersion}/misarchExperimentConfig")
    suspend fun putMisarchExperimentConfig(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
        @RequestBody misarchExperimentConfig: String,
    ) = experimentConfigService.updateMisarchExperimentConfig(testUUID, testVersion, misarchExperimentConfig)

    @GetMapping("/experiment/{testUUID}/{testVersion}/gatlingConfig")
    suspend fun getGatlingConfigs(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
    ): List<EncodedFileDTO> = experimentConfigService.getGatlingConfigs(testUUID, testVersion)

    @PutMapping("/experiment/{testUUID}/{testVersion}/gatlingConfig")
    suspend fun putGatlingConfigs(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
        @RequestBody configs: List<EncodedFileDTO>,
    ) = experimentConfigService.updateGatlingConfigs(testUUID, testVersion, configs)

    @GetMapping("/experiment/{testUUID}/{testVersion}/config")
    suspend fun getExperimentConfig(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
    ): ExperimentConfig = experimentConfigService.getExperimentConfig(testUUID, testVersion)

    @PutMapping("/experiment/{testUUID}/{testVersion}/config")
    suspend fun putExperimentConfig(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
        @RequestBody experimentConfig: ExperimentConfig,
    ) = experimentConfigService.updateExperimentConfig(testUUID, testVersion, experimentConfig)
}
