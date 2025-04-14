package org.misarch.experimentexecutor.controller.experiment

import org.misarch.experimentexecutor.executor.model.ExperimentConfig
import org.misarch.experimentexecutor.service.experiment.ExperimentExecutionService
import org.misarch.experimentexecutor.service.experiment.GraphQLQueryGeneratorService
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
class TestController(
    private val graphQLQueryGeneratorService: GraphQLQueryGeneratorService,
    private val experimentExecutionService: ExperimentExecutionService,
) {
    @GetMapping("/generateGraphQL")
    suspend fun createLayout(@RequestParam graphQLURL: String) {
        graphQLQueryGeneratorService.generateGraphQL(URI(graphQLURL))
    }

    @PostMapping("/experiment")
    suspend fun createExperiment(@RequestBody experimentConfig: ExperimentConfig) {
        experimentExecutionService.executeExperiment(experimentConfig)
    }

    @GetMapping("/trigger/{testUUID}")
    suspend fun trigger(@PathVariable testUUID: UUID): String {
        return experimentExecutionService.getTriggerState(testUUID).toString()
    }
}