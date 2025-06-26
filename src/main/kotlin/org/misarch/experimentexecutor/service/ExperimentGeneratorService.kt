package org.misarch.experimentexecutor.service

import MiSArchFailureConfig
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.misarch.experimentexecutor.config.CHAOSTOOLKIT_FILENAME
import org.misarch.experimentexecutor.config.EXECUTION_FILENAME
import org.misarch.experimentexecutor.config.GATLING_REALISTIC_USER_STEPS_FILENAME
import org.misarch.experimentexecutor.config.GATLING_WORK_FILENAME_ABORT
import org.misarch.experimentexecutor.config.GATLING_WORK_FILENAME_BUY
import org.misarch.experimentexecutor.config.MISARCH_EXPERIMENT_CONFIG_FILENAME
import org.misarch.experimentexecutor.config.TEMPLATE_PREFIX
import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.model.GatlingLoadType
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.model.ChaosToolkitConfig
import org.misarch.experimentexecutor.plugin.workload.gatling.model.GatlingConfig
import org.misarch.experimentexecutor.service.builders.buildChaosToolkitConfig
import org.misarch.experimentexecutor.service.builders.buildEmptyChaosToolkitConfig
import org.misarch.experimentexecutor.service.builders.buildEmptyMisarchExperimentConfig
import org.misarch.experimentexecutor.service.builders.buildExperimentConfig
import org.misarch.experimentexecutor.service.builders.buildMisarchExperimentConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID
import kotlin.collections.forEach

private val logger = KotlinLogging.logger {}

@Service
class ExperimentGeneratorService(
    @Value("\${experiment-executor.base-path}") private val basePath: String,
    @Value("\${experiment-executor.template-path}") private val templatePath: String,
) {
    suspend fun generateExperiment(
        testName: String,
        loadType: GatlingLoadType,
        testDuration: Int,
        maximumArrivingUsersPerSecond: Int,
        rate: Float,
    ): String {
        val testUUID = UUID.randomUUID()
        val testVersion = "v1"

        val experimentConfig = buildExperimentConfig(testUUID, testVersion, testName, loadType)
        val (gatlingConfigs, misarchExperimentConfig, chaosToolkitConfig) =
            when (loadType) {
                GatlingLoadType.NormalLoadTest -> {
                    Triple(
                        generateRealisticGatlingConfigs(testDuration, maximumArrivingUsersPerSecond),
                        buildMisarchExperimentConfig(testDuration),
                        buildChaosToolkitConfig(testUUID, testVersion, testDuration),
                    )
                }

                GatlingLoadType.ScalabilityLoadTest -> {
                    Triple(
                        generateScalabilityGatlingConfigs(testDuration, rate),
                        buildEmptyMisarchExperimentConfig(),
                        buildEmptyChaosToolkitConfig(testUUID, testVersion),
                    )
                }

                GatlingLoadType.ElasticityLoadTest -> {
                    Triple(
                        generateElasticityGatlingConfigs(testDuration, rate),
                        buildEmptyMisarchExperimentConfig(),
                        buildEmptyChaosToolkitConfig(testUUID, testVersion),
                    )
                }

                GatlingLoadType.ResilienceLoadTest -> {
                    Triple(
                        generateResilienceGatlingConfigs(testDuration, rate),
                        buildMisarchExperimentConfig(testDuration),
                        buildChaosToolkitConfig(testUUID, testVersion, testDuration),
                    )
                }
            }

        persistExperiment(testUUID, testVersion, experimentConfig, gatlingConfigs, misarchExperimentConfig, chaosToolkitConfig)
        logger.info { "Experiment generated with testUUID $testUUID and testVersion $testVersion" }

        return "$testUUID:$testVersion"
    }

    private suspend fun generateRealisticGatlingConfigs(
        testDuration: Int,
        maximumArrivingUsersPerSecond: Int,
    ): List<GatlingConfig> {
        val normalUserSteps = File("$templatePath/${TEMPLATE_PREFIX}${GATLING_REALISTIC_USER_STEPS_FILENAME}").readText()
        val values = normalUserSteps.replace("usersteps\n", "").split("\n").map { it.trim().toIntOrNull() ?: 0 }
        val factor = maximumArrivingUsersPerSecond.toFloat() / values.max().toFloat()
        val startIndex = (0..(values.size - testDuration)).random()
        val userSteps = values.subList(startIndex, startIndex + testDuration).map { (it * factor).toInt() }

        val userStepsBuy = userSteps.map { if (it < 2) 0 else (it / 2).coerceAtLeast(0) }
        val userStepsAborted = userSteps.map { if (it < 2) it else (it / 2).coerceAtLeast(0) }

        return listOf(
            GatlingConfig(
                fileName = "buyProcessScenario",
                userStepsFileContent = "usersteps\n" + userStepsBuy.joinToString("\n"),
                workFileContent = File("$templatePath/${TEMPLATE_PREFIX}$GATLING_WORK_FILENAME_BUY").readText(),
            ),
            GatlingConfig(
                fileName = "abortedBuyProcessScenario",
                userStepsFileContent = "usersteps\n" + userStepsAborted.joinToString("\n"),
                workFileContent = File("$templatePath/${TEMPLATE_PREFIX}$GATLING_WORK_FILENAME_ABORT").readText(),
            ),
        )
    }

    private suspend fun generateScalabilityGatlingConfigs(
        testDuration: Int,
        rate: Float,
    ): List<GatlingConfig> {
        val userSteps = List(testDuration) { step -> (step * rate).toInt().coerceAtLeast(1) }
        return listOf(
            GatlingConfig(
                fileName = "buyProcessScenario",
                userStepsFileContent = "usersteps\n" + userSteps.joinToString("\n"),
                workFileContent = File("$templatePath/${TEMPLATE_PREFIX}$GATLING_WORK_FILENAME_BUY").readText(),
            ),
        )
    }

    private suspend fun generateElasticityGatlingConfigs(
        testDuration: Int,
        rate: Float,
    ): List<GatlingConfig> {
        val growth = List(size = testDuration / 6) { step -> (step * rate).toInt().coerceAtLeast(1) }
        val decay = List(size = testDuration / 6) { 1 }
        val userSteps = growth + decay + growth + decay + growth + decay
        return listOf(
            GatlingConfig(
                fileName = "buyProcessScenario",
                userStepsFileContent = "usersteps\n" + userSteps.joinToString("\n"),
                workFileContent = File("$templatePath/${TEMPLATE_PREFIX}$GATLING_WORK_FILENAME_BUY").readText(),
            ),
        )
    }

    private suspend fun generateResilienceGatlingConfigs(
        testDuration: Int,
        rate: Float,
    ): List<GatlingConfig> {
        val growth = List(size = testDuration / 6) { step -> (step * (rate / 2)).toInt().coerceAtLeast(1) }
        val decay = List(size = testDuration / 6) { 1 }
        val spikeUp = List(size = testDuration / 24) { step -> (step * rate * 25).toInt().coerceAtLeast(1) }
        val spikeDown = spikeUp.reversed()
        val lowPlateau = List(size = testDuration / 6) { 1 }
        val highPlateauPattern = listOf(100)
        val highPlateau = List(testDuration / 3) { index -> highPlateauPattern[index % highPlateauPattern.size] }
        val final = List(size = testDuration / 12) { 1 }
        val userSteps = growth + decay + spikeUp + spikeDown + lowPlateau + highPlateau + final

        return listOf(GATLING_WORK_FILENAME_BUY, GATLING_WORK_FILENAME_ABORT).map { scenarioName ->
            GatlingConfig(
                fileName = scenarioName.substringBefore("."),
                userStepsFileContent = "usersteps\n" + userSteps.joinToString("\n"),
                workFileContent = File("$templatePath/${TEMPLATE_PREFIX}$scenarioName").readText(),
            )
        }
    }

    fun persistExperiment(
        testUUID: UUID,
        testVersion: String,
        experimentConfig: ExperimentConfig,
        gatlingConfigs: List<GatlingConfig>,
        misarchExperimentConfig: List<MiSArchFailureConfig>,
        chaosToolkitConfig: ChaosToolkitConfig,
    ) {
        val experimentDir = "$basePath/$testUUID/$testVersion"

        val dirCreated = File(experimentDir).mkdirs()
        if (!dirCreated) {
            throw IllegalStateException("Failed to create directory at $experimentDir")
        }

        File("$experimentDir/$EXECUTION_FILENAME")
            .writeText(jacksonObjectMapper().writeValueAsString(experimentConfig))
        File("$experimentDir/$CHAOSTOOLKIT_FILENAME")
            .writeText(jacksonObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL).writeValueAsString(chaosToolkitConfig))
        File("$experimentDir/$MISARCH_EXPERIMENT_CONFIG_FILENAME")
            .writeText(jacksonObjectMapper().writeValueAsString(misarchExperimentConfig))
        gatlingConfigs.forEach { config ->
            File("$experimentDir/${config.fileName}.kt").writeText(config.workFileContent)
            File("$experimentDir/${config.fileName}.csv").writeText(config.userStepsFileContent)
        }
    }
}
