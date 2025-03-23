package org.misarch.experimentconfignew.service.experiment

import org.misarch.experimentconfignew.executor.model.ExperimentConfig
import org.springframework.stereotype.Service

@Service
class ExperimentExecutionService(
    private val setupExperimentFailureService: SetupExperimentFailureService,
    private val k6LoadTestGenerator: K6LoadTestGenerator
) {
    suspend fun executeExperiment(experimentConfig: ExperimentConfig) {

        // First, execute the load test without any failure modification
        setupExperimentFailureService.resetExperimentFailure()
        val loadTestUri = k6LoadTestGenerator.generateLoadTest(experimentConfig.load)
        k6LoadTestGenerator.executeLoadTest(loadTestUri)

        // Second, execute the load test with the actual configured failure state
        setupExperimentFailureService.setupExperimentFailure(experimentConfig.failure)
        k6LoadTestGenerator.executeLoadTest(loadTestUri)
    }
}