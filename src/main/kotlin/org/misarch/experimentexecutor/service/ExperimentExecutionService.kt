package org.misarch.experimentexecutor.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.misarch.experimentexecutor.config.ExperimentExecutorConfig
import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.service.model.ExperimentState
import org.misarch.experimentexecutor.service.model.ExperimentState.TriggerState.COMPLETED
import org.misarch.experimentexecutor.service.model.ExperimentState.TriggerState.INITIALIZING
import org.misarch.experimentexecutor.service.model.ExperimentState.TriggerState.STARTED
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class ExperimentExecutionService(
    private val experimentConfigService: ExperimentConfigService,
    private val experimentFailureService: ExperimentFailureService,
    private val experimentWorkloadService: ExperimentWorkloadService,
    private val experimentMetricsService: ExperimentMetricsService,
    private val experimentResultService: ExperimentResultService,
    private val experimentExecutorConfig: ExperimentExecutorConfig,
    private val redisCacheService: RedisCacheService,
) {

    suspend fun getTriggerState(testUUID: UUID): Boolean {
        return redisCacheService.retrieveExperimentState(testUUID).triggerState == STARTED
    }

    suspend fun executeStoredExperiment(testUUID: UUID): String {
        val experimentConfig = experimentConfigService.getExperimentConfig(testUUID)
        return executeExperiment(experimentConfig, testUUID)
    }

    suspend fun executeExperiment(experimentConfig: ExperimentConfig, testUUID: UUID): String {

        val experimentState = ExperimentState(testUUID = testUUID, triggerState = INITIALIZING, goals = experimentConfig.goals)
        redisCacheService.cacheExperimentState(experimentState)

        coroutineScope {
            val failureJobs = async {
                experimentFailureService.setupExperimentFailure(experimentConfig.failure, testUUID)
            }

            val workloadJobs = async {
                experimentWorkloadService.executeWorkLoad(experimentConfig.workLoad, testUUID)
            }

            logger.info { "Wait for trigger for test with testUUID: $testUUID" }

            delay(experimentExecutorConfig.triggerDelay)
            redisCacheService.cacheExperimentState(experimentState.copy(triggerState = STARTED, startTime = Instant.now().toString()))

            logger.info { "Started Experiment run for testUUID: $testUUID" }

            experimentFailureService.startExperimentFailure()

            failureJobs.await()
            workloadJobs.await()
        }
        return testUUID.toString()
    }

    suspend fun finishExperiment(testUUID: UUID, gatlingStatsJs: String, gatlingStatsHtml: String) {

        val experimentState = redisCacheService.retrieveExperimentState(testUUID)

        val startTimeString = experimentState.startTime ?: throw IllegalStateException("Experiment start time not found for testUUID: $testUUID")
        val startTime = Instant.parse(startTimeString)
        val endTime = Instant.now().plusSeconds(60)

        redisCacheService.cacheExperimentState(experimentState.copy(endTime = endTime.toString(), triggerState = COMPLETED))

        experimentMetricsService.exportMetrics(testUUID, gatlingStatsJs, gatlingStatsHtml)
        experimentResultService.createAndExportReports(testUUID, startTime, endTime, experimentState.goals)

        logger.info { "Finished Experiment run for testUUID: $testUUID" }
    }
}