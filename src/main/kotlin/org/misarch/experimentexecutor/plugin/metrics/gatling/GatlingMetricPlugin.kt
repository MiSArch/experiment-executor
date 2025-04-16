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

        val stats = parseGatlingStats(workLoad)
        val registry = CollectorRegistry.defaultRegistry
        registry.clear()
        registry.registerGauges(stats)

        val pushGateway = PushGateway("localhost:9091")
        pushGateway.pushAdd(registry, "gatling_metrics", mapOf("testUUID" to testUUID.toString()))

        println("🚀 Metrics pushed to Prometheus Pushgateway")
    }

    private fun parseGatlingStats(workLoad: WorkLoad): GatlingStats {
        val pathUri = workLoad.gatling!!.pathUri
        val latest = File("$pathUri/build/reports/gatling").listFiles()?.filter { it.isDirectory }?.maxOfOrNull { it.name }
        val rawJs = File("$pathUri/build/reports/gatling/$latest/js/stats.js").readText()
        val json = rawJs.trimGatlingJs()

        return ObjectMapper().readValue(json, GatlingStats::class.java)
    }

    private fun String.trimGatlingJs(): String =
        removePrefix("var stats = ").split("function fillStats").first().replace("stats:", "\"stats\":")
            .replace("type:", "\"type\":").replace("name:", "\"name\":").replace("path:", "\"path\":")
            .replace("pathFormatted:", "\"pathFormatted\":").replace("contents:", "\"contents\":")

    private fun CollectorRegistry.register(name: String, help: String) =
        Gauge.build().name(name).help(help).register(this)

    private fun CollectorRegistry.registerGauges(stats: GatlingStats) {

        register("gatling_number_of_requests_total", "Total number of requests").set(stats.stats.numberOfRequests.total.toDouble())
        register("gatling_number_of_requests_ok", "Number of successful requests").set(stats.stats.numberOfRequests.ok.toDouble())
        register("gatling_number_of_requests_ko", "Number of failed requests").set(stats.stats.numberOfRequests.ko.toDouble())

        register("gatling_mean_response_time", "Mean response time").set(stats.stats.meanResponseTime.total.toDouble())
        register("gatling_mean_response_time_ok", "Mean response time for successful requests").set(stats.stats.meanResponseTime.ok.toDouble())
        register("gatling_mean_response_time_ko", "Mean response time for failed requests").set(stats.stats.meanResponseTime.ko.toDouble())

        register("gatling_min_response_time", "Min response time").set(stats.stats.minResponseTime.total.toDouble())
        register("gatling_min_response_time_ok", "Min response time for successful requests").set(stats.stats.minResponseTime.ok.toDouble())
        register("gatling_min_response_time_ko", "Min response time for failed requests").set(stats.stats.minResponseTime.ko.toDouble())

        register("gatling_max_response_time", "Max response time").set(stats.stats.maxResponseTime.total.toDouble())
        register("gatling_max_response_time_ok", "Max response time for successful requests").set(stats.stats.maxResponseTime.ok.toDouble())
        register("gatling_max_response_time_ko", "Max response time for failed requests").set(stats.stats.maxResponseTime.ko.toDouble())

        register("gatling_standard_deviation", "Standard deviation of response time").set(stats.stats.standardDeviation.total.toDouble())
        register("gatling_standard_deviation_ok", "Standard deviation for successful requests").set(stats.stats.standardDeviation.ok.toDouble())
        register("gatling_standard_deviation_ko", "Standard deviation for failed requests").set(stats.stats.standardDeviation.ko.toDouble())

        register("gatling_mean_requests_per_second", "Mean number of requests per second").set(stats.stats.meanNumberOfRequestsPerSecond.total.toDouble())
        register("gatling_mean_requests_per_second_ok", "Mean number of successful requests per second").set(stats.stats.meanNumberOfRequestsPerSecond.ok.toDouble())
        register("gatling_mean_requests_per_second_ko", "Mean number of failed requests per second").set(stats.stats.meanNumberOfRequestsPerSecond.ko.toDouble())

        // Percentiles
        register("gatling_percentiles1", "Percentiles 1").set(stats.stats.percentiles1.total.toDouble())
        register("gatling_percentiles1_ok", "Percentiles 1 for successful requests").set(stats.stats.percentiles1.ok.toDouble())
        register("gatling_percentiles1_ko", "Percentiles 1 for failed requests").set(stats.stats.percentiles1.ko.toDouble())

        register("gatling_percentiles2", "Percentiles 2").set(stats.stats.percentiles2.total.toDouble())
        register("gatling_percentiles2_ok", "Percentiles 2 for successful requests").set(stats.stats.percentiles2.ok.toDouble())
        register("gatling_percentiles2_ko", "Percentiles 2 for failed requests").set(stats.stats.percentiles2.ko.toDouble())

        register("gatling_percentiles3", "Percentiles 3").set(stats.stats.percentiles3.total.toDouble())
        register("gatling_percentiles3_ok", "Percentiles 3 for successful requests").set(stats.stats.percentiles3.ok.toDouble())
        register("gatling_percentiles3_ko", "Percentiles 3 for failed requests").set(stats.stats.percentiles3.ko.toDouble())

        register("gatling_percentiles4", "Percentiles 4").set(stats.stats.percentiles4.total.toDouble())
        register("gatling_percentiles4_ok", "Percentiles 4 for successful requests").set(stats.stats.percentiles4.ok.toDouble())
        register("gatling_percentiles4_ko", "Percentiles 4 for failed requests").set(stats.stats.percentiles4.ko.toDouble())

        // Groups
        register("gatling_group1_count", "Group 1 count").set(stats.stats.group1.count.toDouble())
        register("gatling_group2_count", "Group 2 count").set(stats.stats.group2.count.toDouble())
        register("gatling_group3_count", "Group 3 count").set(stats.stats.group3.count.toDouble())
        register("gatling_group4_count", "Group 4 count").set(stats.stats.group4.count.toDouble())
    }
}