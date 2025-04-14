package org.misarch.experimentexecutor.service.experiment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.misarch.experimentexecutor.executor.model.ExperimentConfig
import org.springframework.stereotype.Service
import java.util.*

@Service
class ExperimentExecutionService(
    private val experimentFailureService: ExperimentFailureService,
    private val experimentWorkloadService: ExperimentWorkloadService,
) {
    companion object {
        const val TRIGGER_DELAY = 10000L
    }

    private val triggerState: MutableMap<UUID, Boolean> = mutableMapOf()

    suspend fun getTriggerState(testUUID: UUID): Boolean {
        return triggerState[testUUID] ?: false
    }

    suspend fun executeExperiment(experimentConfig: ExperimentConfig) {
        val testUUID = UUID.randomUUID()
        triggerState[testUUID] = false

        withContext(Dispatchers.Default) {
            val failureJob = async {
                experimentFailureService.setupExperimentFailure(experimentConfig.failure, testUUID)
            }

            val workloadJob = async {
                experimentWorkloadService.executeWorkLoad(experimentConfig.workLoad, testUUID)
            }

            delay(TRIGGER_DELAY)
            triggerState[testUUID] = true
            experimentFailureService.startExperimentFailure()

            failureJob.await()
            workloadJob.await()
        }
    }
}