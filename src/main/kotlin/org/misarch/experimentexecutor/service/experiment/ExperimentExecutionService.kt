package org.misarch.experimentexecutor.service.experiment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.misarch.experimentexecutor.executor.model.ExperimentConfig
import org.springframework.stereotype.Service

@Service
class ExperimentExecutionService(
    private val experimentFailureService: ExperimentFailureService,
    private val experimentWorkloadService: ExperimentWorkloadService,
) {
    suspend fun executeExperiment(experimentConfig: ExperimentConfig) {
        // Reset failure service before starting
        experimentFailureService.resetExperimentFailure()

        withContext(Dispatchers.Default) {
            // Create two separate async blocks to run in parallel
            val failureJob = async {
                experimentFailureService.setupExperimentFailure(experimentConfig.failure)
            }

            val workloadJob = async {
                experimentWorkloadService.executeWorkLoad(experimentConfig.workLoad)
            }

            // Wait for both operations to complete
            failureJob.await()
            workloadJob.await()
        }
    }
}