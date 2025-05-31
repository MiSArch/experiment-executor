package org.misarch.experimentexecutor.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.misarch.experimentexecutor.plugin.metrics.MetricPluginInterface
import org.misarch.experimentexecutor.plugin.metrics.gatling.GatlingMetricsPlugin
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Service
class ExperimentMetricsService(
    webClient: WebClient,
    @Value("\${influxdb.url}") private val influxUrl: String,
    @Value("\${pushgateway.url}") private val pushGatewayUrl: String,
) : MetricPluginInterface {

    private val registry = listOf(
        GatlingMetricsPlugin(webClient, influxUrl, pushGatewayUrl),
    )

    suspend fun exportMetrics(testUUID: UUID, testVersion: String, gatlingStatsJs: String, gatlingStatsHtml: String) {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.exportMetrics(testUUID, testVersion, gatlingStatsJs, gatlingStatsHtml) }
            }
        }.awaitAll()
    }
}