package org.misarch.experimentexecutor.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.misarch.experimentexecutor.config.*
import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.model.GatlingLoadType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
class ExperimentConfigService(
    @Value("\${experiment-executor.base-path}") private val basePath: String,
    @Value("\${experiment-executor.template-path}") private val templatePath: String,
    @Value("\${misarch.experiment-config.host}") private val misarchExperimentConfigHost: String,
    @Value("\${gatling.target-endpoint}") private val gatlingTargetEndpoint: String,
) {
    suspend fun generateExperiment(loadType: GatlingLoadType, testDuration: Int, sessionDuration: Int, rate: Float): String {
        val testUUID = UUID.randomUUID()
        val experimentDir = "$basePath/$testUUID"

        val dirCreated = File(experimentDir).mkdirs()
        if (!dirCreated) {
            throw IllegalStateException("Failed to create directory at $experimentDir")
        }

        generateUserStepsCSV(experimentDir, testUUID, loadType, testDuration, sessionDuration, rate)

        val chaostoolkitTemplate = File("$templatePath/${TEMPLATE_PREFIX}${CHAOSTOOLKIT_FILENAME}").readText()
        val updatedChaostoolkitTemplate = chaostoolkitTemplate.replace("REPLACE_ME_TEST_UUID", testUUID.toString())
        File("$experimentDir/$CHAOSTOOLKIT_FILENAME").writeText(updatedChaostoolkitTemplate)

        val misarchTemplate = File("$templatePath/${TEMPLATE_PREFIX}${MISARCH_EXPERIMENT_CONFIG_FILENAME}").readText()
        File("$experimentDir/$MISARCH_EXPERIMENT_CONFIG_FILENAME").writeText(misarchTemplate)

        val workTemplate = File("$templatePath/${TEMPLATE_PREFIX}${GATLING_WORK_FILENAME}").readText()
        File("$experimentDir/$GATLING_WORK_FILENAME").writeText(workTemplate)

        val executionTemplate = File("$templatePath/${TEMPLATE_PREFIX}${EXECUTION_FILENAME}").readText()
        val executionTemplateUpdated = executionTemplate
            .replace("REPLACE_ME_TEST_UUID", testUUID.toString())
            .replace("REPLACE_ME_BASE_PATH", experimentDir)
            .replace("REPLACE_ME_LOADTYPE", loadType.toString())
            .replace("REPLACE_ME_CHAOSTOOLKIT_FILENAME", CHAOSTOOLKIT_FILENAME)
            .replace("REPLACE_ME_MISARCH_EXPERIMENT_CONFIG_ENDPOINT", misarchExperimentConfigHost)
            .replace("REPLACE_ME_MISARCH_EXPERIMENT_CONFIG_FILENAME", MISARCH_EXPERIMENT_CONFIG_FILENAME)
            .replace("REPLACE_ME_GATLING_TARGET_ENDPOINT", gatlingTargetEndpoint)
            .replace("REPLACE_ME_GATLING_WORK_FILENAME", GATLING_WORK_FILENAME)
            .replace("REPLACE_ME_GATLING_USERSTEPS_FILENAME", GATLING_USERSTEPS_FILENAME)
        File("$experimentDir/$EXECUTION_FILENAME").writeText(executionTemplateUpdated)

        return testUUID.toString()
    }

    fun getChaosToolkitConfig(testUUID: UUID): String {
        return File("$basePath/$testUUID/$CHAOSTOOLKIT_FILENAME").readText()
    }

    fun updateChaosToolkitConfig(testUUID: UUID, chaosToolKitConfig: String) {
        File("$basePath/$testUUID/$CHAOSTOOLKIT_FILENAME").writeText(chaosToolKitConfig)
    }

    fun getMisarchExperimentConfig(testUUID: UUID): String {
        return File("$basePath/$testUUID/$MISARCH_EXPERIMENT_CONFIG_FILENAME").readText()
    }

    fun updateMisarchExperimentConfig(testUUID: UUID, misarchExperimentConfig: String) {
        val filePath = "$basePath/$testUUID/$MISARCH_EXPERIMENT_CONFIG_FILENAME"
        File(filePath).writeText(misarchExperimentConfig)
    }

    fun getGatlingUsersteps(testUUID: UUID): String {
        return File("$basePath/$testUUID/$GATLING_USERSTEPS_FILENAME").readText()
    }

    fun updateGatlingUsersteps(testUUID: UUID, usersteps: String) {
        val filePath = "$basePath/$testUUID/$GATLING_USERSTEPS_FILENAME"
        File(filePath).writeText(usersteps)
    }

    fun getExperimentConfig(testUUID: UUID): ExperimentConfig {
        val rawText = File("$basePath/$testUUID/$EXECUTION_FILENAME").readText()
        return jacksonObjectMapper().readValue(rawText, ExperimentConfig::class.java)
    }

    fun updateExperimentConfig(testUUID: UUID, experimentConfig: ExperimentConfig) {
        val filePath = "$basePath/$testUUID/$EXECUTION_FILENAME"
        val jsonContent = jacksonObjectMapper().writeValueAsString(experimentConfig)
        File(filePath).writeText(jsonContent)
    }

    fun getGatlingWork(testUUID: UUID): String {
        return File("$basePath/$testUUID/$GATLING_WORK_FILENAME").readText()
    }

    fun updateGatlingWork(testUUID: UUID, work: String) {
        val filePath = "$basePath/$testUUID/$GATLING_WORK_FILENAME"
        File(filePath).writeText(work)
    }

    fun generateUserStepsCSV(
        experimentDir: String,
        testUUID: UUID,
        loadType: GatlingLoadType,
        testDuration: Int,
        sessionDuration: Int,
        rate: Float
    ) {
        val userSteps = when (loadType) {
            GatlingLoadType.NormalLoadTest -> {
                val normalUsersteps = File("$templatePath/${TEMPLATE_PREFIX}${GATLING_USERSTEPS_FILENAME}").readText()
                val values = normalUsersteps.replace("usersteps\n", "").split("\n").map { it.trim().toIntOrNull() ?: 0 }
                values.subList(0, testDuration)
            }

            GatlingLoadType.ScalabilityLoadTest -> {
                List(testDuration) { step -> (step * rate).toInt().coerceAtLeast(sessionDuration) }
            }

            GatlingLoadType.ElasticityLoadTest -> {
                val growth = List(size = testDuration / 6) { step -> (step * rate).toInt().coerceAtLeast(sessionDuration) }
                val decay = List(size = testDuration / 6) { sessionDuration }
                growth + decay + growth + decay + growth + decay
            }

            GatlingLoadType.ResilienceLoadTest -> {
                val growth = List(size = testDuration / 6) { step -> (step * (rate/2)).toInt().coerceAtLeast(sessionDuration) }
                val decay = List(size = testDuration / 6) { sessionDuration }
                val spikeUp = List(size = testDuration / 24) { step -> (step * rate * 25).toInt().coerceAtLeast(sessionDuration) }
                val spikeDown = spikeUp.reversed()
                val lowPlateau = List(size = testDuration / 6) { sessionDuration }
                val highPlateauPattern = listOf(sessionDuration * 50) + List(size = sessionDuration - 1) { 0 }
                val highPlateau = List(testDuration / 3) { index -> highPlateauPattern[index % highPlateauPattern.size] }
                val final = List(size = testDuration / 12) { sessionDuration }
                growth + decay + spikeUp + spikeDown + lowPlateau + highPlateau + final
            }
        }

        File("$experimentDir/$GATLING_USERSTEPS_FILENAME").writeText("usersteps\n" + userSteps.joinToString("\n"))
    }
}