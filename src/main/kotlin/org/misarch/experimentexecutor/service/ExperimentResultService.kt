package org.misarch.experimentexecutor.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.misarch.experimentexecutor.config.GrafanaConfig
import org.misarch.experimentexecutor.model.Goal
import org.misarch.experimentexecutor.plugin.export.grafana.GrafanaPlugin
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.*

@Service
class ExperimentResultService(
    webClient: WebClient,
    grafanaConfig: GrafanaConfig,
    @Value("\${experiment-executor.template-path}") private val templatePath: String,
) {

    val registry = listOf(
        GrafanaPlugin(webClient, grafanaConfig, templatePath),
    )

    suspend fun createAndExportReports(testUUID: UUID, testVersion: String, startTime: Instant, endTime: Instant, goals: List<Goal>) {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.createReport(testUUID, testVersion, startTime, endTime, goals) }
            }
        }.awaitAll()
    }
}