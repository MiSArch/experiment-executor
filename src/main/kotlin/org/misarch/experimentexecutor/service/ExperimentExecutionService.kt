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

    suspend fun getTriggerState(testUUID: UUID, testVersion: String): Boolean {
        return redisCacheService.retrieveExperimentState(testUUID, testVersion).triggerState == STARTED
    }

    suspend fun executeStoredExperiment(testUUID: UUID, testVersion: String): String {
        val experimentConfig = experimentConfigService.getExperimentConfig(testUUID, testVersion)
        return executeExperiment(experimentConfig, testUUID, testVersion)
    }

    suspend fun executeExperiment(experimentConfig: ExperimentConfig, testUUID: UUID, testVersion: String): String {

        val experimentState = ExperimentState(
            testUUID = testUUID,
            testVersion = testVersion,
            triggerState = INITIALIZING,
            goals = experimentConfig.goals
        )

        redisCacheService.cacheExperimentState(experimentState)

        CoroutineScope(Dispatchers.Default).launch {
            val failureJobs = async {
                experimentFailureService.setupExperimentFailure(testUUID, testVersion)
            }

            val workloadJobs = async {
                experimentWorkloadService.executeWorkLoad(experimentConfig.workLoad, testUUID, testVersion)
            }

            logger.info { "Started experiment and waiting for trigger for testUUID: $testUUID and testVersion: $testVersion" }

            delay(experimentExecutorConfig.triggerDelay)
            redisCacheService.cacheExperimentState(experimentState.copy(triggerState = STARTED, startTime = Instant.now().toString()))

            experimentFailureService.startExperimentFailure(testUUID, testVersion)

            failureJobs.await()
            workloadJobs.await()
        }
        return testUUID.toString()
    }

    suspend fun cancelExperiment(testUUID: UUID, testVersion: String) {
        coroutineScope {
            val failureJob = async { experimentFailureService.stopExperimentFailure(testUUID, testVersion) }
            val workLoadJob = async { experimentWorkloadService.stopWorkLoad(testUUID, testVersion) }

            workLoadJob.await()
            failureJob.await()
        }
        redisCacheService.deleteExperimentState(testUUID, testVersion)
    }

    suspend fun finishExperiment(testUUID: UUID, testVersion: String, gatlingStatsJs: String, gatlingStatsHtml: String) {

        val experimentState = redisCacheService.retrieveExperimentState(testUUID, testVersion)

        val startTimeString = experimentState.startTime
            ?: throw IllegalStateException("Experiment start time not found for testUUID: $testUUID and testVersion: $testVersion")
        val startTime = Instant.parse(startTimeString)
        val endTime = Instant.now().plusSeconds(60)

        redisCacheService.cacheExperimentState(experimentState.copy(endTime = endTime.toString(), triggerState = COMPLETED))

        experimentMetricsService.exportMetrics(testUUID, testVersion, gatlingStatsJs, gatlingStatsHtml)
        experimentResultService.createAndExportReports(testUUID, testVersion, startTime, endTime, experimentState.goals)

        logger.info { "Finished Experiment run for testUUID: $testUUID and testVersion: $testVersion" }
    }
}