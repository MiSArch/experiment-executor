package org.misarch.experimentexecutor.plugin.metrics.prometheus

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import org.misarch.experimentexecutor.plugin.metrics.MetricPluginInterface
import org.misarch.experimentexecutor.plugin.metrics.prometheus.model.PrometheusResponse
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import java.net.URI
import java.net.URLEncoder
import java.time.Instant
import java.util.*
import kotlin.text.Charsets.UTF_8

private val logger = KotlinLogging.logger {}

class PrometheusMetricPlugin(
    private val webClient: WebClient,
    private val influxUrl: String,
) : MetricPluginInterface {
    private val services = listOf(
        "address",
        "catalog",
        "discount",
        "gateway",
        "inventory",
        "invoice",
        "media",
        "notification",
        "order",
        "payment",
        "return",
        "review",
        "shipment",
        "shoppingcart",
        "simulation",
        "tax",
        "user",
        "wishlist",
    )

    private val metrics = services.map { service ->
        mapOf(
            """container_cpu_usage_seconds_total""" to """{name="infrastructure-docker-$service-1"}""",
            """dapr_http_server_latency_sum""" to """{app_id="$service"}""",
            """container_memory_usage_bytes""" to """{name="infrastructure-docker-$service-1"}""",
            """http_server_request_duration_count""" to """{exported_job="$service"}""",
            """container_start_time_seconds""" to """{name="infrastructure-docker-$service-1"}""",
            """dapr_component_pubsub_egress_latencies_sum""" to """{app_id="$service"}""",
            """http_server_request_duration_sum""" to """{exported_job="$service"}""",
            """dapr_http_client_roundtrip_latency_sum""" to """{app_id="$service"}""",
            """dapr_http_client_completed_count""" to """{app_id="$service"}"""
        )
    }

    override suspend fun exportMetrics(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        gatlingStatsJs: String,
        gatlingStatsHtml: String,
    ) {
        metrics.forEach { metricList ->
            metricList.forEach { (metricName, metricFilter) ->
                queryPrometheus(
                    metricName = metricName,
                    metricFilter = metricFilter,
                    start = startTime.toString(),
                    end = endTime.toString(),
                    step = "1s",
                    testUUID = testUUID,
                    testVersion = testVersion
                )
            }
        }
        logger.info { "🚀 Prometheus Metrics pushed to InfluxDB for Comparison" }
    }

    private suspend fun queryPrometheus(
        metricName: String,
        metricFilter: String,
        start: String, end:
        String, step: String,
        testUUID: UUID,
        testVersion: String
    ) {
        val uri = URI.create(
            "http://localhost:9090/api/v1/query_range?" +
                    "query=${URLEncoder.encode(metricName + metricFilter, UTF_8)}" +
                    "&start=${URLEncoder.encode(start, UTF_8)}" +
                    "&end=${URLEncoder.encode(end, UTF_8)}" +
                    "&step=${URLEncoder.encode(step, UTF_8)}"
        )

        val response = webClient.get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(PrometheusResponse::class.java)
            .awaitSingle()

        val values = response.data.result.firstOrNull()?.values
        if (values.isNullOrEmpty()) {
            return
        }

        val data = mutableMapOf<Long, Double?>()
        for (i in 1 until values.size) {
            val value = values[i]
            val datapoint = value[1].toDoubleOrNull()
            // Epoch time 2000-01-01T00:00:00Z
            data[(946684800 + i) * 1000L] = datapoint
        }

        val service = services.first { metricFilter.contains(it) }
        if (data.isNotEmpty()) {
            val lineProtocol = data.map { (timestamp, value) ->
                "$metricName,testUUID=$testUUID,testVersion=$testVersion,service=$service value=${value} $timestamp"
            }.joinToString("\n")
            postToInflux(lineProtocol)
        }
    }

    private suspend fun postToInflux(lineProtocol: String) {
        webClient.post()
            .uri(influxUrl)
            .header("Authorization", "Token my-secret-token")  // TODO CONFIG
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue(lineProtocol)
            .retrieve()
            .awaitBodilessEntity()
    }
}