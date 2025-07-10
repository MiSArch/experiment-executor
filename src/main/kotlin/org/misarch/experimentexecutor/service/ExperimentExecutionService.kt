package org.misarch.experimentexecutor.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.misarch.experimentexecutor.controller.experiment.AsyncEventErrorHandler
import org.misarch.experimentexecutor.controller.experiment.AsyncEventResponder
import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.plugin.metrics.gatling.extractGoals
import org.misarch.experimentexecutor.service.model.ExperimentState
import org.misarch.experimentexecutor.service.model.ExperimentState.TriggerState.COMPLETED
import org.misarch.experimentexecutor.service.model.ExperimentState.TriggerState.INITIALIZING
import org.misarch.experimentexecutor.service.model.ExperimentState.TriggerState.STARTED
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

@Service
class ExperimentExecutionService(
    private val experimentConfigService: ExperimentConfigService,
    private val experimentFailureService: ExperimentFailureService,
    private val experimentWorkloadService: ExperimentWorkloadService,
    private val experimentMetricsService: ExperimentMetricsService,
    private val experimentResultService: ExperimentResultService,
    private val asyncEventErrorHandler: AsyncEventErrorHandler,
    private val asyncEventResponder: AsyncEventResponder,
) {
    private val registeredClients = ConcurrentHashMap<String, List<String>>()
    private val experimentStateCache = ConcurrentHashMap<String, ExperimentState>()

    suspend fun getTriggerState(
        testUUID: UUID,
        testVersion: String,
    ): Boolean = getExperimentStateCache(testUUID, testVersion)?.triggerState == STARTED

    suspend fun registerTriggerState(
        testUUID: UUID,
        testVersion: String,
        client: String,
    ) {
        logger.info { "Registering client: $client for testUUID: $testUUID and testVersion: $testVersion" }
        registeredClients["$testUUID:$testVersion"] = registeredClients.getOrDefault("$testUUID:$testVersion", emptyList()) + client
    }

    suspend fun executeStoredExperiment(
        testUUID: UUID,
        testVersion: String,
    ) {
        val experimentConfig = experimentConfigService.getExperimentConfig(testUUID, testVersion)
        initializeExperiment(experimentConfig, testUUID, testVersion)
    }

    suspend fun initializeExperiment(
        experimentConfig: ExperimentConfig,
        testUUID: UUID,
        testVersion: String,
    ) {
        if (getExperimentStateCache(testUUID, testVersion) != null) {
            throw IllegalStateException("An experiment run with testUUID $testUUID and testVersion $testVersion is already in progress.")
        }

        val newExperimentState =
            ExperimentState(
                testUUID = testUUID,
                testVersion = testVersion,
                triggerState = INITIALIZING,
                goals = experimentConfig.goals,
            )

        setExperimentStateCache(newExperimentState)

        val exceptionHandler = buildExceptionHandler(testUUID, testVersion)

        CoroutineScope(Dispatchers.Default + exceptionHandler).launch {
            val testDelay = (experimentConfig.warmUp?.duration ?: 0) + (experimentConfig.steadyState?.duration ?: 0)
            val failureJobs =
                async {
                    experimentFailureService.setupExperimentFailure(testUUID, testVersion, testDelay)
                }

            val workloadJobs =
                async {
                    experimentWorkloadService.executeWorkLoad(testUUID, testVersion, experimentConfig.warmUp, experimentConfig.steadyState)
                }

            logger.info { "Initialized experiment and waiting for registrations for testUUID: $testUUID and testVersion: $testVersion" }

            0.until(6000 + testDelay).forEach { _ ->
                if (registeredClients["$testUUID:$testVersion"]?.contains("gatling") == true &&
                    registeredClients["$testUUID:$testVersion"]?.contains("chaostoolkit") == true &&
                    registeredClients["$testUUID:$testVersion"]?.contains("misarchExperimentConfig") == true
                ) {
                    registeredClients.remove("$testUUID:$testVersion")
                    val experimentState =
                        getExperimentStateCache(testUUID, testVersion)
                            ?: throw IllegalStateException(
                                "Experiment state not found for testUUID: $testUUID and testVersion: $testVersion",
                            )
                    setExperimentStateCache(experimentState.copy(triggerState = STARTED, startTime = Instant.now().toString()))
                    return@forEach
                }
                delay(100)
            }

            failureJobs.await()
            workloadJobs.await()
        }
    }

    suspend fun cancelExperiment(
        testUUID: UUID,
        testVersion: String,
    ) {
        // TODO a retry back-off would help because the container might not be there anymore
        coroutineScope {
            val failureJob = async { experimentFailureService.stopExperimentFailure(testUUID, testVersion) }
            val workLoadJob = async { experimentWorkloadService.stopWorkLoad(testUUID, testVersion) }

            workLoadJob.await()
            failureJob.await()
        }
        registeredClients.remove("$testUUID:$testVersion")
        deleteExperimentStateCache(testUUID, testVersion)
    }

    suspend fun finishSteadyState(
        testUUID: UUID,
        testVersion: String,
        gatlingStatsJs: String,
    ) {
        val exceptionHandler = buildExceptionHandler(testUUID, testVersion)

        CoroutineScope(Dispatchers.Default + exceptionHandler).launch {
            val experimentState =
                getExperimentStateCache(testUUID, testVersion)
                    ?: throw IllegalStateException("Experiment state not found for testUUID: $testUUID and testVersion: $testVersion")

            setExperimentStateCache(experimentState.copy(goals = extractGoals(gatlingStatsJs)))
            logger.info { "Finished steady state analysis for testUUID: $testUUID and testVersion: $testVersion" }
        }
    }

    suspend fun finishExperiment(
        testUUID: UUID,
        testVersion: String,
        gatlingStatsJs: String,
        gatlingStatsHtml: String,
    ) {
        val exceptionHandler = buildExceptionHandler(testUUID, testVersion)

        CoroutineScope(Dispatchers.Default + exceptionHandler).launch {
            val experimentState =
                getExperimentStateCache(testUUID, testVersion)
                    ?: throw IllegalStateException("Experiment state not found for testUUID: $testUUID and testVersion: $testVersion")

            val startTimeString =
                experimentState.startTime
                    ?: throw IllegalStateException("Experiment start time not found for testUUID: $testUUID and testVersion: $testVersion")
            val startTime = Instant.parse(startTimeString)
            val endTime = Instant.now().plusSeconds(60)

            setExperimentStateCache(experimentState.copy(endTime = endTime.toString(), triggerState = COMPLETED))

            experimentMetricsService.exportMetrics(testUUID, testVersion, startTime, endTime, gatlingStatsJs, gatlingStatsHtml)
            experimentResultService.createAndExportReports(
                testUUID,
                testVersion,
                startTime,
                endTime,
                experimentState.goals,
                gatlingStatsHtml,
            )

            asyncEventResponder.emitSuccess(testUUID, testVersion)
            deleteExperimentStateCache(testUUID, testVersion)

            logger.info { "Finished Experiment run for testUUID: $testUUID and testVersion: $testVersion" }
        }
    }

    private suspend fun getExperimentStateCache(
        testUUID: UUID,
        testVersion: String,
    ): ExperimentState? = experimentStateCache["$testUUID:$testVersion"]

    private suspend fun setExperimentStateCache(experimentState: ExperimentState) {
        experimentStateCache["${experimentState.testUUID}:${experimentState.testVersion}"] = experimentState
    }

    private suspend fun deleteExperimentStateCache(
        testUUID: UUID,
        testVersion: String,
    ) {
        experimentStateCache.remove("$testUUID:$testVersion")
    }

    private fun buildExceptionHandler(
        testUUID: UUID,
        testVersion: String,
    ) = CoroutineExceptionHandler { _, throwable ->
        logger.error(throwable) { "Async failure for testUUID: $testUUID and testVersion: $testVersion" }
        asyncEventErrorHandler.handleError(testUUID, testVersion, throwable.message ?: "Unknown error")
        CoroutineScope(Dispatchers.Default).launch {
            cancelExperiment(testUUID, testVersion)
        }
    }
}
