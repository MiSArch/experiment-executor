package org.misarch.experimentexecutor.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.misarch.experimentexecutor.config.CHAOSTOOLKIT_FILENAME
import org.misarch.experimentexecutor.config.EXECUTION_FILENAME
import org.misarch.experimentexecutor.config.GATLING_USERSTEPS_FILENAME_1
import org.misarch.experimentexecutor.config.GATLING_USERSTEPS_FILENAME_2
import org.misarch.experimentexecutor.config.GATLING_WORK_FILENAME_1
import org.misarch.experimentexecutor.config.GATLING_WORK_FILENAME_2
import org.misarch.experimentexecutor.config.MISARCH_EXPERIMENT_CONFIG_FILENAME
import org.misarch.experimentexecutor.config.TEMPLATE_PREFIX
import org.misarch.experimentexecutor.controller.experiment.model.EncodedFileDTO
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
        val updatedChaostoolkitTemplate = chaostoolkitTemplate
            .replace("REPLACE_ME_TEST_UUID", testUUID.toString())
            .replace("REPLACE_ME_TEST_VERSION", testVersion)
        File("$experimentDir/$CHAOSTOOLKIT_FILENAME").writeText(updatedChaostoolkitTemplate)

        val misarchTemplate = File("$templatePath/${TEMPLATE_PREFIX}${MISARCH_EXPERIMENT_CONFIG_FILENAME}").readText()
        File("$experimentDir/$MISARCH_EXPERIMENT_CONFIG_FILENAME").writeText(misarchTemplate)

        val workTemplate1 = File("$templatePath/${TEMPLATE_PREFIX}${GATLING_WORK_FILENAME_1}").readText()
        File("$experimentDir/$GATLING_WORK_FILENAME_1").writeText(workTemplate1)

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

    fun getGatlingConfigs(
        testUUID: UUID,
        testVersion: String,
    ): List<EncodedFileDTO> {
        val files = File("$basePath/$testUUID/$testVersion")
        val fileNames =
            files
                .listFiles()
                ?.filter { it.isFile && it.name.endsWith(".kt") }
                ?.map { it.name }
                ?: emptyList()

        return fileNames.map { workFileName ->
            val fileName = workFileName.removeSuffix(".kt")
            val userStepsFileName = "$fileName.csv"
            val workFileContent = File("$basePath/$testUUID/$testVersion/$workFileName").readText()
            val userStepsFileContent = File("$basePath/$testUUID/$testVersion/$userStepsFileName").readText()
            EncodedFileDTO(
                fileName = fileName,
                encodedWorkFileContent = Base64.encode(workFileContent.toByteArray(Charsets.UTF_8)),
                encodedUserStepsFileContent = Base64.encode(userStepsFileContent.toByteArray(Charsets.UTF_8)),
            )
        }
    }

    fun updateGatlingConfigs(
        testUUID: UUID,
        testVersion: String,
        configs: List<EncodedFileDTO>,
    ) {
        val files = File("$basePath/$testUUID/$testVersion")
        val dtoFileNames = configs.map { it.fileName }.toSet()
        val fileNames =
            files
                .listFiles()
                ?.filter { it.isFile && it.name.endsWith(".kt") }
                ?.map { it.name }
                ?: emptyList()

        fileNames.forEach { fileName ->
            val filePath = "$basePath/$testUUID/$testVersion/$fileName"
            File(filePath).delete()
        }

        files.listFiles()
            ?.filter { file ->
                file.isFile && (file.name.endsWith(".csv") || file.name.endsWith(".kt") && dtoFileNames.none {
                    file.name.startsWith(
                        file.name,
                    )
                })
            }
            ?.forEach { it.delete() }

        val filePath = "$basePath/$testUUID/$testVersion"
        configs.forEachIndexed { i, dto ->
            val decodedWorkContent = Base64.decode(dto.encodedWorkFileContent).decodeToString()
            val decodedUserStepsContent = Base64.decode(dto.encodedUserStepsFileContent).decodeToString()
            File("$filePath/${dto.fileName}.kt").writeText(decodedWorkContent)
            File("$filePath/${dto.fileName}.csv").writeText(decodedUserStepsContent)
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
                // TODO think of something nice how to create multiple types for usersteps by default
                //  e.g.: when realistic 70 / 30 aborted / successful +++ elasticity only one +++ resilience one with random spike
                GatlingLoadType.NormalLoadTest -> {
                    val normalUsersteps = File("$templatePath/${TEMPLATE_PREFIX}${GATLING_USERSTEPS_FILENAME_1}").readText()
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

        // TODO finalize this
        File("$experimentDir/$GATLING_USERSTEPS_FILENAME_1").writeText("usersteps\n" + userSteps.joinToString("\n"))
        File("$experimentDir/$GATLING_USERSTEPS_FILENAME_2").writeText("usersteps\n" + userSteps.reversed().joinToString("\n"))
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
