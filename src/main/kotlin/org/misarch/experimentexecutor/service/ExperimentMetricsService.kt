package org.misarch.experimentexecutor.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.misarch.experimentexecutor.plugin.metrics.gatling.GatlingMetricsPlugin
import org.misarch.experimentexecutor.plugin.metrics.prometheus.PrometheusMetricPlugin
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.*

@Service
class ExperimentMetricsService(
    webClient: WebClient,
    @Value("\${influxdb.url}") private val influxUrl: String,
) {

    private val registry = listOf(
        GatlingMetricsPlugin(webClient, influxUrl),
        PrometheusMetricPlugin(webClient),
    )

    suspend fun exportMetrics(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        gatlingStatsJs: String,
        gatlingStatsHtml: String
    ) {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.exportMetrics(testUUID, testVersion, startTime, endTime, gatlingStatsJs, gatlingStatsHtml) }
            }
        }.awaitAll()
    }
}