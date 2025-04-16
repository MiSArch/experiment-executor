package org.misarch.experimentexecutor.plugin.metrics.gatling

import com.fasterxml.jackson.databind.ObjectMapper
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge
import io.prometheus.client.exporter.PushGateway
import org.misarch.experimentexecutor.executor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.metrics.MetricPluginInterface
import org.misarch.experimentexecutor.plugin.metrics.gatling.model.GatlingStats
import java.io.File
import java.util.UUID

class GatlingMetricPlugin : MetricPluginInterface {

    fun collectAndExportMetrics(workLoad: WorkLoad, testUUID: UUID) {

        val responseTimeStats = parseGatlingResponseTimeStats(workLoad)
        val registry = CollectorRegistry.defaultRegistry
        registry.clear()
        registry.registerResponseTimeGauges(responseTimeStats)
        responseTimeStats.contents?.forEach { (request, requestStats) ->
            registry.registerResponseTimeGauges(requestStats, suffix = "_${request.split("-").first().replace("-", "_")}")
        }

        val pushGateway = PushGateway("localhost:9091")
        pushGateway.pushAdd(registry, "gatling_metrics", mapOf("testUUID" to testUUID.toString()))

        println("🚀 Metrics pushed to Prometheus Pushgateway")
    }

    private fun parseGatlingResponseTimeStats(workLoad: WorkLoad): GatlingStats {
        val pathUri = workLoad.gatling!!.pathUri
        val latest =
            File("$pathUri/build/reports/gatling").listFiles()?.filter { it.isDirectory }?.maxOfOrNull { it.name }
        val rawJs = File("$pathUri/build/reports/gatling/$latest/js/stats.js").readText()
        val json = rawJs.trimGatlingJs()

        return ObjectMapper().readValue(json, GatlingStats::class.java)
    }

    private fun String.trimGatlingJs(): String =
        removePrefix("var stats = ").split("function fillStats").first().replace("stats:", "\"stats\":")
            .replace("type:", "\"type\":").replace("name:", "\"name\":").replace("path:", "\"path\":")
            .replace("pathFormatted:", "\"pathFormatted\":").replace("contents:", "\"contents\":")

    private fun CollectorRegistry.registerResponseTimeGauges(stats: GatlingStats, suffix: String = "") {

        stats.stats.numberOfRequests.total.toDoubleOrNull()?.let { register("gatling_number_of_requests_total$suffix", "Total number of requests").set(it) }
        stats.stats.numberOfRequests.ok.toDoubleOrNull()?.let { register("gatling_number_of_requests_ok$suffix", "Number of successful requests").set(it) }
        stats.stats.numberOfRequests.ko.toDoubleOrNull()?.let { register("gatling_number_of_requests_ko$suffix", "Number of failed requests").set(it) }

        stats.stats.meanResponseTime.total.toDoubleOrNull()?.let { register("gatling_mean_response_time$suffix", "Mean response time").set(it) }
        stats.stats.meanResponseTime.ok.toDoubleOrNull()?.let { register("gatling_mean_response_time_ok$suffix", "Mean response time for successful requests").set(it) }
        stats.stats.meanResponseTime.ko.toDoubleOrNull()?.let { register("gatling_mean_response_time_ko$suffix", "Mean response time for failed requests").set(it) }

        stats.stats.minResponseTime.total.toDoubleOrNull()?.let { register("gatling_min_response_time$suffix", "Min response time").set(it) }
        stats.stats.minResponseTime.ok.toDoubleOrNull()?.let { register("gatling_min_response_time_ok$suffix", "Min response time for successful requests").set(it) }
        stats.stats.minResponseTime.ko.toDoubleOrNull()?.let { register("gatling_min_response_time_ko$suffix", "Min response time for failed requests").set(it) }

        stats.stats.maxResponseTime.total.toDoubleOrNull()?.let { register("gatling_max_response_time$suffix", "Max response time").set(it) }
        stats.stats.maxResponseTime.ok.toDoubleOrNull()?.let { register("gatling_max_response_time_ok$suffix", "Max response time for successful requests").set(it) }
        stats.stats.maxResponseTime.ko.toDoubleOrNull()?.let { register("gatling_max_response_time_ko$suffix", "Max response time for failed requests").set(it) }

        stats.stats.standardDeviation.total.toDoubleOrNull()?.let { register("gatling_standard_deviation$suffix", "Standard deviation of response time").set(it) }
        stats.stats.standardDeviation.ok.toDoubleOrNull()?.let { register("gatling_standard_deviation_ok$suffix", "Standard deviation for successful requests").set(it) }
        stats.stats.standardDeviation.ko.toDoubleOrNull()?.let { register("gatling_standard_deviation_ko$suffix", "Standard deviation for failed requests").set(it) }

        stats.stats.meanNumberOfRequestsPerSecond.total.toDoubleOrNull()?.let { register("gatling_mean_requests_per_second$suffix", "Mean number of requests per second").set(it) }
        stats.stats.meanNumberOfRequestsPerSecond.ok.toDoubleOrNull()?.let { register("gatling_mean_requests_per_second_ok$suffix", "Mean number of successful requests per second").set(it) }
        stats.stats.meanNumberOfRequestsPerSecond.ko.toDoubleOrNull()?.let { register("gatling_mean_requests_per_second_ko$suffix", "Mean number of failed requests per second").set(it) }

        // Percentiles
        stats.stats.percentiles1.total.toDoubleOrNull()?.let { register("gatling_percentiles1$suffix", "Percentiles 1").set(it) }
        stats.stats.percentiles1.ok.toDoubleOrNull()?.let { register("gatling_percentiles1_ok$suffix", "Percentiles 1 for successful requests").set(it) }
        stats.stats.percentiles1.ko.toDoubleOrNull()?.let { register("gatling_percentiles1_ko$suffix", "Percentiles 1 for failed requests").set(it) }

        stats.stats.percentiles2.total.toDoubleOrNull()?.let { register("gatling_percentiles2$suffix", "Percentiles 2").set(it) }
        stats.stats.percentiles2.ok.toDoubleOrNull()?.let { register("gatling_percentiles2_ok$suffix", "Percentiles 2 for successful requests").set(it) }
        stats.stats.percentiles2.ko.toDoubleOrNull()?.let { register("gatling_percentiles2_ko$suffix", "Percentiles 2 for failed requests").set(it) }

        stats.stats.percentiles3.total.toDoubleOrNull()?.let { register("gatling_percentiles3$suffix", "Percentiles 3").set(it) }
        stats.stats.percentiles3.ok.toDoubleOrNull()?.let { register("gatling_percentiles3_ok$suffix", "Percentiles 3 for successful requests").set(it) }
        stats.stats.percentiles3.ko.toDoubleOrNull()?.let { register("gatling_percentiles3_ko$suffix", "Percentiles 3 for failed requests").set(it) }

        stats.stats.percentiles4.total.toDoubleOrNull()?.let { register("gatling_percentiles4$suffix", "Percentiles 4").set(it) }
        stats.stats.percentiles4.ok.toDoubleOrNull()?.let { register("gatling_percentiles4_ok$suffix", "Percentiles 4 for successful requests").set(it) }
        stats.stats.percentiles4.ko.toDoubleOrNull()?.let { register("gatling_percentiles4_ko$suffix", "Percentiles 4 for failed requests").set(it) }

        // Groups
        register("gatling_group1_count$suffix", "Group 1 count").set(stats.stats.group1.count.toDouble())
        register("gatling_group2_count$suffix", "Group 2 count").set(stats.stats.group2.count.toDouble())
        register("gatling_group3_count$suffix", "Group 3 count").set(stats.stats.group3.count.toDouble())
        register("gatling_group4_count$suffix", "Group 4 count").set(stats.stats.group4.count.toDouble())
    }

    private fun CollectorRegistry.register(name: String, help: String) =
        Gauge.build().name(name).help(help).register(this)
}