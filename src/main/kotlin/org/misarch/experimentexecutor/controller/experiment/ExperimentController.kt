package org.misarch.experimentexecutor.controller.experiment

import org.misarch.experimentexecutor.executor.model.ExperimentConfig
import org.misarch.experimentexecutor.executor.model.GatlingLoadType
import org.misarch.experimentexecutor.service.experiment.ExperimentConfigService
import org.misarch.experimentexecutor.service.experiment.ExperimentExecutionService
import org.misarch.experimentexecutor.service.experiment.GraphQLQueryGeneratorService
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@CrossOrigin(origins = ["http://localhost:5173"])
class ExperimentController(
    private val graphQLQueryGeneratorService: GraphQLQueryGeneratorService,
    private val experimentExecutionService: ExperimentExecutionService,
    private val experimentConfigService: ExperimentConfigService,
) {
    @GetMapping("/generateGraphQL")
    suspend fun createLayout(@RequestParam graphQLURL: String) {
        graphQLQueryGeneratorService.generateGraphQL(URI(graphQLURL))
    }

    @PostMapping("/experiment/generate/{loadType}")
    suspend fun generateExperiment(@PathVariable loadType: GatlingLoadType): String {
        return experimentConfigService.generateExperiment(loadType)
    }

    @PostMapping("/experiment")
    suspend fun runExperimentWithConfigFile(@RequestBody experimentConfig: ExperimentConfig): String {
        return experimentExecutionService.executeExperiment(experimentConfig, UUID.fromString(experimentConfig.testUUID))
    }

    @PostMapping("/experiment/{testUUID}")
    suspend fun runExperiment(@PathVariable testUUID: UUID): String {
        return experimentExecutionService.executeStoredExperiment(testUUID)
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
    suspend fun getMisarchExperimentConfig(@PathVariable testUUID: UUID,): String {
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
    suspend fun putGatlingUsersteps(@PathVariable testUUID: UUID, @RequestBody usersteps: String) {
        return experimentConfigService.updateGatlingUsersteps(testUUID, usersteps)
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

    @GetMapping("/trigger/{testUUID}")
    suspend fun trigger(@PathVariable testUUID: UUID): String {
        return experimentExecutionService.getTriggerState(testUUID).toString()
    }
}