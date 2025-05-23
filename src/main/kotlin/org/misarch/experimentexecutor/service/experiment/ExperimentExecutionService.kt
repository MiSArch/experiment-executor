package org.misarch.experimentexecutor.service.experiment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.misarch.experimentexecutor.executor.model.ExperimentConfig
import org.misarch.experimentexecutor.executor.model.GatlingLoadType
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.util.*

@Service
class ExperimentExecutionService(
    private val experimentFailureService: ExperimentFailureService,
    private val experimentWorkloadService: ExperimentWorkloadService,
    private val experimentMetricsService: ExperimentMetricsService,
    private val experimentResultService: ExperimentResultService,
) {
    companion object {
        const val TRIGGER_DELAY = 10000L
    }

    private val triggerState: MutableMap<UUID, Boolean> = mutableMapOf()

    suspend fun getTriggerState(testUUID: UUID): Boolean {
        return triggerState[testUUID] ?: false
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

            experimentMetricsService.collectAndExportMetrics(experimentConfig.workLoad, testUUID)
            experimentResultService.createAndExportReports(testUUID, startTime, endTime, experimentConfig.goals)

        }
        return testUUID.toString()
    }
}