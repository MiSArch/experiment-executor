package org.misarch.experimentexecutor.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.misarch.experimentexecutor.config.GrafanaConfig
import org.misarch.experimentexecutor.model.Goal
import org.misarch.experimentexecutor.plugin.export.grafana.GrafanaPlugin
import org.misarch.experimentexecutor.plugin.export.llm.LLMPlugin
import org.misarch.experimentexecutor.plugin.export.report.ReportPlugin
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.UUID

@Service
class ExperimentResultService(
    webClient: WebClient,
    grafanaConfig: GrafanaConfig,
    @Value("\${experiment-executor.template-path}") private val templatePath: String,
    @Value("\${experiment-executor.base-path}") private val basePath: String,
    @Value("\${experiment-executor.is-kubernetes}") private val isKubernetes: Boolean,
    buildProperties: BuildProperties,
) {
    val registry =
        listOf(
            GrafanaPlugin(webClient, grafanaConfig, templatePath, basePath, isKubernetes),
            ReportPlugin(basePath, buildProperties),
            LLMPlugin(), // not implemented yet
        )

    suspend fun createAndExportReports(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        goals: List<Goal>,
        gatlingStatsHtml: String,
    ) {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.createReport(testUUID, testVersion, startTime, endTime, goals, gatlingStatsHtml) }
            }
        }.awaitAll()
    }
}
