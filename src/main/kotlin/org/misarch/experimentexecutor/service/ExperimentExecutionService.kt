package org.misarch.experimentexecutor.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.misarch.experimentexecutor.config.ExperimentExecutorConfig
import org.misarch.experimentexecutor.controller.error.AsyncEventErrorHandler
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
    private val asyncEventErrorHandler: AsyncEventErrorHandler,
) {

    suspend fun getTriggerState(testUUID: UUID, testVersion: String): Boolean {
        return redisCacheService.retrieveExperimentState(testUUID, testVersion).triggerState == STARTED
    }

    suspend fun executeStoredExperiment(testUUID: UUID, testVersion: String, endpointAccessToken: String?) {
        val experimentConfig = experimentConfigService.getExperimentConfig(testUUID, testVersion)
        return if (endpointAccessToken == null) {
            executeExperiment(experimentConfig, testUUID, testVersion)
        } else {
            executeExperiment(
                experimentConfig.copy(
                    workLoad = experimentConfig.workLoad.copy(
                        gatling = experimentConfig.workLoad.gatling.copy
                            (endpointAccessToken = endpointAccessToken)
                    )
                ),
                testUUID,
                testVersion
            )
        }
    }

    suspend fun executeExperiment(experimentConfig: ExperimentConfig, testUUID: UUID, testVersion: String) {

        val experimentState = ExperimentState(
            testUUID = testUUID,
            testVersion = testVersion,
            triggerState = INITIALIZING,
            goals = experimentConfig.goals
        )

        redisCacheService.cacheExperimentState(experimentState)

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            logger.error(throwable) { "Failed to execute experiment for testUUID: $testUUID and testVersion: $testVersion" }
            asyncEventErrorHandler.handleError(testUUID, testVersion, throwable.message ?: "Unknown error")
        }

        CoroutineScope(Dispatchers.Default + exceptionHandler).launch {
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
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            logger.error(throwable) { "Failed to finish experiment for testUUID: $testUUID and testVersion: $testVersion" }
            asyncEventErrorHandler.handleError(testUUID, testVersion, throwable.message ?: "Unknown error")
        }

        CoroutineScope(Dispatchers.Default + exceptionHandler).launch {
            val experimentState = redisCacheService.retrieveExperimentState(testUUID, testVersion)

            val startTimeString = experimentState.startTime
                ?: throw IllegalStateException("Experiment start time not found for testUUID: $testUUID and testVersion: $testVersion")
            val startTime = Instant.parse(startTimeString)
            val endTime = Instant.now().plusSeconds(60)

            redisCacheService.cacheExperimentState(experimentState.copy(endTime = endTime.toString(), triggerState = COMPLETED))

            experimentMetricsService.exportMetrics(testUUID, testVersion, startTime, endTime, gatlingStatsJs, gatlingStatsHtml)
            experimentResultService.createAndExportReports(testUUID, testVersion, startTime, endTime, experimentState.goals)

            logger.info { "Finished Experiment run for testUUID: $testUUID and testVersion: $testVersion" }
        }
    }
}