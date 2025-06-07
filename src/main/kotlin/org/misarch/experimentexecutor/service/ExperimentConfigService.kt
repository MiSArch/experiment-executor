package org.misarch.experimentexecutor.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.misarch.experimentexecutor.config.CHAOSTOOLKIT_FILENAME
import org.misarch.experimentexecutor.config.EXECUTION_FILENAME
import org.misarch.experimentexecutor.config.GATLING_USERSTEPS_FILENAME
import org.misarch.experimentexecutor.config.GATLING_WORK_FILENAME
import org.misarch.experimentexecutor.config.GATLING_WORK_FILENAME_1
import org.misarch.experimentexecutor.config.GATLING_WORK_FILENAME_2
import org.misarch.experimentexecutor.config.MISARCH_EXPERIMENT_CONFIG_FILENAME
import org.misarch.experimentexecutor.config.TEMPLATE_PREFIX
import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.model.GatlingLoadType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Service
@OptIn(ExperimentalEncodingApi::class)
class ExperimentConfigService(
    @Value("\${experiment-executor.base-path}") private val basePath: String,
    @Value("\${experiment-executor.template-path}") private val templatePath: String,
    @Value("\${gatling.target-endpoint}") private val gatlingTargetEndpoint: String,
) {
    suspend fun getExistingExperiments(): List<String> {
        val experimentsDir = File(basePath)
        return experimentsDir
            .listFiles()
            ?.filter { it.isDirectory && isValidUUID(it.name) }
            ?.map { it.name }
            ?: emptyList()
    }

    private fun isValidUUID(name: String): Boolean =
        runCatching {
            UUID.fromString(name)
            true
        }.getOrDefault(false)


    suspend fun getExperimentVersions(testUUID: UUID): List<String> {
        val testDir = File("$basePath/$testUUID")
        return testDir
            .listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("v") }
            ?.map { it.name }
            ?: emptyList()
    }

    suspend fun generateExperiment(
        testName: String,
        loadType: GatlingLoadType,
        testDuration: Int,
        sessionDuration: Int,
        rate: Float,
    ): String {
        val testUUID = UUID.randomUUID()
        val testVersion = "v1"
        val experimentDir = "$basePath/$testUUID/$testVersion"

        val dirCreated = File(experimentDir).mkdirs()
        if (!dirCreated) {
            throw IllegalStateException("Failed to create directory at $experimentDir")
        }

        generateUserStepsCSV(experimentDir, loadType, testDuration, sessionDuration, rate)

        val chaostoolkitTemplate = File("$templatePath/${TEMPLATE_PREFIX}${CHAOSTOOLKIT_FILENAME}").readText()
        val updatedChaostoolkitTemplate = chaostoolkitTemplate.replace("REPLACE_ME_TEST_UUID", testUUID.toString())
        File("$experimentDir/$CHAOSTOOLKIT_FILENAME").writeText(updatedChaostoolkitTemplate)

        val misarchTemplate = File("$templatePath/${TEMPLATE_PREFIX}${MISARCH_EXPERIMENT_CONFIG_FILENAME}").readText()
        File("$experimentDir/$MISARCH_EXPERIMENT_CONFIG_FILENAME").writeText(misarchTemplate)

        val workTemplate = File("$templatePath/${TEMPLATE_PREFIX}${GATLING_WORK_FILENAME_1}").readText()
        File("$experimentDir/$GATLING_WORK_FILENAME_1").writeText(workTemplate)

        val workTemplate2 = File("$templatePath/${TEMPLATE_PREFIX}${GATLING_WORK_FILENAME_2}").readText()
        File("$experimentDir/$GATLING_WORK_FILENAME_2").writeText(workTemplate2)

        val executionTemplate = File("$templatePath/${TEMPLATE_PREFIX}${EXECUTION_FILENAME}").readText()
        val executionTemplateUpdated =
            executionTemplate
                .replace("REPLACE_ME_TEST_UUID", testUUID.toString())
                .replace("REPLACE_ME_TEST_VERSION", testVersion)
                .replace("REPLACE_ME_TEST_NAME", testName)
                .replace("REPLACE_ME_LOADTYPE", loadType.toString())
                .replace("REPLACE_ME_GATLING_TARGET_ENDPOINT", gatlingTargetEndpoint)
        File("$experimentDir/$EXECUTION_FILENAME").writeText(executionTemplateUpdated)

        return "$testUUID:$testVersion"
    }

    fun getChaosToolkitConfig(
        testUUID: UUID,
        testVersion: String,
    ): String = File("$basePath/$testUUID/$testVersion/$CHAOSTOOLKIT_FILENAME").readText()

    fun updateChaosToolkitConfig(
        testUUID: UUID,
        testVersion: String,
        chaosToolKitConfig: String,
    ) {
        File("$basePath/$testUUID/$testVersion/$CHAOSTOOLKIT_FILENAME").writeText(chaosToolKitConfig)
    }

    fun getMisarchExperimentConfig(
        testUUID: UUID,
        testVersion: String,
    ): String = File("$basePath/$testUUID/$testVersion/$MISARCH_EXPERIMENT_CONFIG_FILENAME").readText()

    fun updateMisarchExperimentConfig(
        testUUID: UUID,
        testVersion: String,
        misarchExperimentConfig: String,
    ) {
        val filePath = "$basePath/$testUUID/$testVersion/$MISARCH_EXPERIMENT_CONFIG_FILENAME"
        File(filePath).writeText(misarchExperimentConfig)
    }

    fun getGatlingUsersteps(
        testUUID: UUID,
        testVersion: String,
    ): String = File("$basePath/$testUUID/$testVersion/$GATLING_USERSTEPS_FILENAME").readText()

    fun updateGatlingUsersteps(
        testUUID: UUID,
        testVersion: String,
        usersteps: String,
    ) {
        val filePath = "$basePath/$testUUID/$testVersion/$GATLING_USERSTEPS_FILENAME"
        File(filePath).writeText(usersteps)
    }

    fun getExperimentConfig(
        testUUID: UUID,
        testVersion: String,
    ): ExperimentConfig {
        val rawText = File("$basePath/$testUUID/$testVersion/$EXECUTION_FILENAME").readText()
        return jacksonObjectMapper().readValue(rawText, ExperimentConfig::class.java)
    }

    fun updateExperimentConfig(
        testUUID: UUID,
        testVersion: String,
        experimentConfig: ExperimentConfig,
    ) {
        val filePath = "$basePath/$testUUID/$testVersion/$EXECUTION_FILENAME"
        val jsonContent = jacksonObjectMapper().writeValueAsString(experimentConfig)
        File(filePath).writeText(jsonContent)
    }

    fun getGatlingWork(
        testUUID: UUID,
        testVersion: String,
    ): List<String> {
        val files = File("$basePath/$testUUID/$testVersion")
        val fileNames = files
            .listFiles()
            ?.filter { it.isFile && it.name.endsWith(".kt") }
            ?.map { it.name }
            ?: emptyList()

        return fileNames.map { fileName ->
            val filePath = "$basePath/$testUUID/$testVersion/$fileName"
            val fileContent = File(filePath).readText()
            Base64.encode(fileContent.toByteArray(Charsets.UTF_8))
        }
    }

    fun updateGatlingWork(
        testUUID: UUID,
        testVersion: String,
        work: List<String>,
    ) {
        val files = File("$basePath/$testUUID/$testVersion")
        val fileNames = files
            .listFiles()
            ?.filter { it.isFile && it.name.endsWith(".kt") }
            ?.map { it.name }
            ?: emptyList()

        fileNames.forEach { fileName ->
            val filePath = "$basePath/$testUUID/$testVersion/$fileName"
            File(filePath).delete()
        }
        val filePath = "$basePath/$testUUID/$testVersion"
        work.forEachIndexed { i, encodedValue ->
            val decodedContent = Base64.decode(encodedValue).decodeToString()
            val fileName = "${GATLING_WORK_FILENAME}-${i+1}.kt"
            File("$filePath/$fileName").writeText(decodedContent)
        }
    }

    fun generateUserStepsCSV(
        experimentDir: String,
        loadType: GatlingLoadType,
        testDuration: Int,
        sessionDuration: Int,
        rate: Float,
    ) {
        val userSteps =
            when (loadType) {
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
                    val growth = List(size = testDuration / 6) { step -> (step * (rate / 2)).toInt().coerceAtLeast(sessionDuration) }
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

    suspend fun newExperimentVersion(
        testUUID: UUID,
        testVersion: String,
    ): String {
        val currentVersionDir = File("$basePath/$testUUID/$testVersion")
        val testDir = File("$basePath/$testUUID")

        val versionPattern = Regex("v(\\d+)")
        val highestVersion =
            testDir
                .listFiles()
                ?.mapNotNull {
                    versionPattern
                        .find(it.name)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()
                }?.maxOrNull() ?: 1

        val newVersion = "v${highestVersion + 1}"
        val newVersionDir = File("$basePath/$testUUID/$newVersion")
        if (!newVersionDir.mkdirs()) {
            throw IllegalStateException("Failed to create new version directory: $newVersionDir")
        }

        currentVersionDir.listFiles()?.forEach { file ->
            file.copyTo(File(newVersionDir, file.name), overwrite = true)
        }

        val executionFile = File("$newVersionDir/$EXECUTION_FILENAME")
        val experimentConfig = jacksonObjectMapper().readValue(executionFile, ExperimentConfig::class.java)
        val updatedConfig = experimentConfig.copy(testVersion = newVersion)
        executionFile.writeText(jacksonObjectMapper().writeValueAsString(updatedConfig))

        return newVersion
    }
}
