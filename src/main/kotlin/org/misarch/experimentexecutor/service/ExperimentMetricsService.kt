package org.misarch.experimentexecutor.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.misarch.experimentexecutor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.metrics.MetricPluginInterface
import org.misarch.experimentexecutor.plugin.metrics.gatling.GatlingMetricPlugin
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

    // TODO implement a plugin registry based on a configuration file
    private val registry = listOf(
        GatlingMetricPlugin(webClient, influxUrl, pushGatewayUrl),
    )

    suspend fun collectAndExportMetrics(workLoad: WorkLoad, testUUID: UUID) {
        supervisorScope {
            registry.map { plugin ->
                async { plugin.collectAndExportMetrics(workLoad, testUUID) }
            }
        }.awaitAll()
    }
}