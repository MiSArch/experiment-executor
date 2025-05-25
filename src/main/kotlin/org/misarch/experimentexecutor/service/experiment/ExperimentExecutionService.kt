package org.misarch.experimentexecutor.service.experiment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.misarch.experimentexecutor.model.ExperimentConfig
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class ExperimentExecutionService(
    private val experimentConfigService: ExperimentConfigService,
    private val experimentFailureService: ExperimentFailureService,
    private val experimentWorkloadService: ExperimentWorkloadService,
    private val experimentMetricsService: ExperimentMetricsService,
    private val experimentResultService: ExperimentResultService,
) {
    companion object {
        // TODO CONFIGURABLE
        const val TRIGGER_DELAY = 15000L
    }

    private val triggerState: MutableMap<UUID, Boolean> = mutableMapOf()

    suspend fun getTriggerState(testUUID: UUID): Boolean {
        return triggerState[testUUID] ?: false
    }

    suspend fun executeStoredExperiment(testUUID: UUID): String {
        val experimentConfig = experimentConfigService.getExperimentConfig(testUUID)
        return executeExperiment(experimentConfig, testUUID)
    }

    suspend fun executeExperiment(experimentConfig: ExperimentConfig, testUUID: UUID): String {
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

            val startTime = Instant.now()

            experimentFailureService.startExperimentFailure()

            failureJob.await()
            workloadJob.await()

            val endTime = Instant.now().plusSeconds(60)

            delay(5000L) // Wait for services to finish end export metrics
            experimentMetricsService.collectAndExportMetrics(experimentConfig.workLoad, testUUID)
            experimentResultService.createAndExportReports(testUUID, startTime, endTime, experimentConfig.goals)

        }
        return testUUID.toString()
    }
}