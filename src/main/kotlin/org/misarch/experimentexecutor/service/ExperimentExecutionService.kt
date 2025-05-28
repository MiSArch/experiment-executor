package org.misarch.experimentexecutor.service

import kotlinx.coroutines.*
import org.misarch.experimentexecutor.config.ExperimentExecutorConfig
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
    private val experimentExecutorConfig: ExperimentExecutorConfig,
) {

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

        coroutineScope {
            val failureJobs = async {
                experimentFailureService.setupExperimentFailure(experimentConfig.failure, testUUID)
            }

            val workloadJobs = async {
                experimentWorkloadService.executeWorkLoad(experimentConfig.workLoad, testUUID)
            }

            delay(experimentExecutorConfig.triggerDelay)
            triggerState[testUUID] = true

            val startTime = Instant.now()

            experimentFailureService.startExperimentFailure()

            failureJobs.await()
            workloadJobs.await()

            val endTime = Instant.now().plusSeconds(60)

            delay(5000L) // Wait for services to finish end export metrics
            experimentMetricsService.collectAndExportMetrics(experimentConfig.workLoad, testUUID)
            experimentResultService.createAndExportReports(testUUID, startTime, endTime, experimentConfig.goals)

        }
        return testUUID.toString()
    }
}