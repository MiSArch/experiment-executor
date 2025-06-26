package org.misarch.experimentexecutor.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.misarch.experimentexecutor.config.CHAOSTOOLKIT_FILENAME
import org.misarch.experimentexecutor.config.EXECUTION_FILENAME
import org.misarch.experimentexecutor.config.MISARCH_EXPERIMENT_CONFIG_FILENAME
import org.misarch.experimentexecutor.controller.experiment.model.EncodedFileDTO
import org.misarch.experimentexecutor.model.ExperimentConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val logger = KotlinLogging.logger {}

@Service
@OptIn(ExperimentalEncodingApi::class)
class ExperimentConfigService(
    @Value("\${experiment-executor.base-path}") private val basePath: String,
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

    fun deleteExperiment(testUUID: UUID) {
        val testDir = File("$basePath/$testUUID")
        if (testDir.exists()) {
            testDir.deleteRecursively()
        }
        logger.info { "Experiment with UUID $testUUID deleted successfully." }
    }

    fun deleteExperimentVersion(
        testUUID: UUID,
        testVersion: String,
    ) {
        val testDir = File("$basePath/$testUUID")
        val versions = testDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("v") } ?: emptyList()
        if (versions.size <= 1) {
            testDir.deleteRecursively()
        } else {
            val versionDir = File("$basePath/$testUUID/$testVersion")
            if (versionDir.exists()) {
                versionDir.deleteRecursively()
            }
        }
        logger.info { "Experiment with testUUID $testUUID and version $testVersion deleted successfully." }
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

        files
            .listFiles()
            ?.filter { file ->
                file.isFile &&
                    (
                        file.name.endsWith(".csv") ||
                            file.name.endsWith(".kt") &&
                            dtoFileNames.none {
                                file.name.startsWith(
                                    file.name,
                                )
                            }
                    )
            }?.forEach { it.delete() }

        val filePath = "$basePath/$testUUID/$testVersion"
        configs.forEachIndexed { i, dto ->
            val decodedWorkContent = Base64.decode(dto.encodedWorkFileContent).decodeToString()
            val decodedUserStepsContent = Base64.decode(dto.encodedUserStepsFileContent).decodeToString()
            File("$filePath/${dto.fileName}.kt").writeText(decodedWorkContent)
            File("$filePath/${dto.fileName}.csv").writeText(decodedUserStepsContent)
        }
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
