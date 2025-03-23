package org.misarch.experimentconfignew.controller.experiment

import org.misarch.experimentconfignew.executor.model.ExperimentConfig
import org.misarch.experimentconfignew.service.experiment.ExperimentExecutionService
import org.misarch.experimentconfignew.service.experiment.GraphQLQueryGeneratorService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

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
}