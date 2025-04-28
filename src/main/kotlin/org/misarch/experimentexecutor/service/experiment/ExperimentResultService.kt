package org.misarch.experimentexecutor.service.experiment

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.misarch.experimentexecutor.plugin.result.grafana.GrafanaPlugin
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.*

@Service
class ExperimentResultService(webClient: WebClient, @Value("\${grafana.apitoken}") private val grafanaApiToken: String) {

    // TODO implement a plugin registry based on a configuration file
    val registry = listOf(
        GrafanaPlugin(webClient, grafanaApiToken),
    )

    suspend fun createAndExportReports(testUUID: UUID, startTime: Instant, endTime: Instant) {
        supervisorScope {
            registry.map { plugin ->
                async { plugin.createReport(testUUID, startTime, endTime) }
            }
        }.awaitAll()
    }
}