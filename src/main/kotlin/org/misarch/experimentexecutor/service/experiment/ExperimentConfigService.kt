package org.misarch.experimentexecutor.service.experiment

import org.misarch.experimentexecutor.executor.model.GatlingLoadType
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
class ExperimentConfigService {

    companion object {
        const val TEMPLATE_PATH = "src/main/resources/templates"
    }

    suspend fun generateExperiment(loadType: GatlingLoadType): String {
        val testUUID = UUID.randomUUID()
        val basePath = System.getenv("BASE_PATH")
        val experimentDir = "$basePath/$testUUID"

        val dirCreated = File(experimentDir).mkdirs()
        if (!dirCreated) {
            throw IllegalStateException("Failed to create directory at $experimentDir")
        }

        val experimentExecutorHost = System.getenv("EXPERIMENT_EXECUTOR_HOST")
        val chaostoolkitTemplate = File("$TEMPLATE_PATH/chaostoolkit-experiment-config-template.yaml").readText()
        val updatedChaostoolkitTemplate = chaostoolkitTemplate.replace("REPLACE_ME_TEST_UUID", testUUID.toString())
            .replace("REPLACE_ME_EXPERIMENT_EXECUTOR_HOST", experimentExecutorHost)
        File("$experimentDir/chaostoolkit-experiment-config.yaml").writeText(updatedChaostoolkitTemplate)

        val misarchTemplate = File("$TEMPLATE_PATH/misarch-experiment-config-template.json").readText()
        File("$experimentDir/misarch-experiment-config.json").writeText(misarchTemplate)

        val userstepsTemplate = File("$TEMPLATE_PATH/gatling-usersteps-template.csv").readText()
        File("$experimentDir/gatling-usersteps.csv").writeText(userstepsTemplate)

        // todo make work configurable

        val executionTemplate = File("$TEMPLATE_PATH/execution-template.json").readText()
        val executionTemplateUpdated = executionTemplate
            .replace("REPLACE_ME_TEST_UUID", testUUID.toString())
            .replace("REPLACE_ME_BASE_PATH", experimentDir)
            .replace("REPLACE_ME_LOADTYPE", loadType.toString())
            // TODO ENDPOINTS!!!
            .replace("REPLACE_ME_MISARCH_EXPERIMENT_CONFIG_ENDPOINT", "http://localhost:3000")
            .replace("REPLACE_ME_GATLING_TARGET_ENDPOINT", "http://localhost:8080")
            .replace("REPLACE_ME_GATLING_TOKEN_ENDPOINT", "http://localhost:8081")
        File("$experimentDir/execution.json").writeText(executionTemplateUpdated)

        return testUUID.toString()
    }

    fun getChaosToolkitConfig(testUUID: UUID): String {
        val basePath = System.getenv("BASE_PATH")
        val experimentDir = "$basePath/$testUUID"
        return File("$experimentDir/chaostoolkit-experiment-config.yaml").readText()    }

    fun updateChaosToolkitConfig(testUUID: UUID, chaosToolKitConfig: String): String {
        TODO("Not yet implemented")
    }

    fun getMisarchExperimentConfig(testUUID: UUID): String {
        val basePath = System.getenv("BASE_PATH")
        val experimentDir = "$basePath/$testUUID"
        return File("$experimentDir/misarch-experiment-config.json").readText()
    }

    fun updateMisarchExperimentConfig(testUUID: UUID, misarchExperimentConfig: String): String {
        TODO("Not yet implemented")
    }

    fun getGatlingUsersteps(testUUID: UUID): String {
        val basePath = System.getenv("BASE_PATH")
        val experimentDir = "$basePath/$testUUID"
        return File("$experimentDir/gatling-usersteps.csv").readText()
    }

    fun updateGatlingUsersteps(testUUID: UUID, usersteps: String): String {
        TODO("Not yet implemented")
    }

    fun getGatlingLoadConfig(testUUID: UUID): String {
        TODO("Not yet implemented")
    }

    fun updateGatlingLoadConfig(testUUID: UUID, loadConfig: String): String {
        TODO("Not yet implemented")
    }

    fun getGatlingWork(testUUID: UUID): String {
        TODO("Not yet implemented")
    }

    fun updateGatlingWork(testUUID: UUID, work: String): String {
        TODO("Not yet implemented")
    }
}