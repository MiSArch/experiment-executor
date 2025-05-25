package org.misarch.experimentexecutor.plugin.metrics.gatling

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge
import io.prometheus.client.exporter.PushGateway
import org.misarch.experimentexecutor.executor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.metrics.MetricPluginInterface
import org.misarch.experimentexecutor.plugin.metrics.gatling.model.GatlingStats
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import java.io.File
import java.util.UUID

class GatlingMetricPlugin(private val webClient: WebClient) : MetricPluginInterface {

    suspend fun collectAndExportMetrics(workLoad: WorkLoad, testUUID: UUID) {

        val pathUri = workLoad.gatling?.pathUri ?: (System.getenv("BASE_PATH") + "/" + testUUID.toString())

        val responseTimeStats = parseGatlingResponseTimeStats(pathUri)
        val registry = CollectorRegistry.defaultRegistry
        registry.clear()

        registry.registerResponseTimeGauges(responseTimeStats)
        responseTimeStats.contents?.forEach { (request, requestStats) ->
            registry.registerResponseTimeGauges(requestStats, suffix = "_${request.split("-").first().replace("-", "_")}")
        }

        val indexPath = "$pathUri/gatling-index.html"

        // TODO responsetimepercentilesovertimeokPercentiles -> list of maps with timestamp and list which represent the response time percentiles at the timepoint
        val dataSeries = extractDataSeries(indexPath)
        dataSeries.forEach { (series, data) ->
            if (series == "requests" || series == "responses") {
                postTotalOkKoSeriesToInflux(data, series, testUUID)
            }
        }

        val activeUsers = extractActiveUsers(indexPath)
        activeUsers.forEach { (scenario, data) -> postActiveUsersToInflux(data, scenario, "activeUsers", testUUID) }

        println("🚀 Gatling Metrics pushed to InfluxDB")

        val pushGateway = PushGateway("localhost:9091")
        pushGateway.pushAdd(registry, "gatling_metrics", mapOf("testUUID" to testUUID.toString()))

        println("🚀 Gatling Metrics pushed to Prometheus Pushgateway")
    }

    private fun parseGatlingResponseTimeStats(pathUri: String): GatlingStats {
        val rawJs = File("$pathUri/gatling-stats.js").readText()
        val json = rawJs.trimGatlingStatsJs()

        return ObjectMapper().readValue(json, GatlingStats::class.java)
    }

    private fun extractDataSeries(filePath: String): Map<String, Map<Long, List<Int>>> {
        val fileContent = File(filePath).readText()

        val dataSeries = mutableMapOf<String, Map<Long, List<Int>>>()
        val regex = """(?s)var (\w+)\s*=\s*unpack\(\s*(\[.*?])\s*\);""".toRegex()

        regex.findAll(fileContent).forEach { matchResult ->
            val variableName = matchResult.groupValues[1]
            val arrayContents = matchResult.groupValues[2]

            val rawList: List<List<Any?>> = ObjectMapper().readValue(arrayContents, object : TypeReference<List<List<Any?>>>() {})

            val valueMap = rawList.associate { pair ->
                val timestamp = (pair[0] as Number).toLong()
                val values = when (val second = pair[1]) {
                    is List<*> -> second.mapNotNull { (it as Number?)?.toInt() }
                    null -> emptyList()
                    else -> error("Unexpected data format: $second")
                }
                timestamp to values
            }.toMap()
            dataSeries[variableName] = valueMap
        }
        return dataSeries
    }

    private fun extractActiveUsers(filePath: String): Map<String, List<Pair<Long, Int>>> {
        val fileContent = File(filePath).readText()

        val regex = Regex(
            """name:\s*'([^']+)'\s*,\s*data:\s*\[((?:\s*\[[^\[\]]+],?\s*)+)]""",
            RegexOption.DOT_MATCHES_ALL
        )

        return regex.findAll(fileContent).map { matchResult ->
            val name = matchResult.groupValues[1] // Extract the name
            val dataArray = "[${matchResult.groupValues[2]}]" // Extract the data array

            val rawData: List<List<Number>> =
                ObjectMapper().readValue(dataArray, object : TypeReference<List<List<Number>>>() {})

            val data = rawData.map { Pair(it[0].toLong(), it[1].toInt()) }

            Pair(name, data)
        }.toMap()
    }


    private fun String.trimGatlingStatsJs(): String =
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

    private suspend fun postActiveUsersToInflux(data: List<Pair<Long, Int>>, scenario: String, metricName: String, testUUID: UUID) {
        val lineProtocol = data.joinToString("\n") { (timestamp, value) ->
            "$metricName,scenario=${scenario.replace(" ", "")},testUUID=$testUUID value=${value.toDouble()} $timestamp"
        }
        postToInflux(lineProtocol)
    }

    private suspend fun postTotalOkKoSeriesToInflux(data: Map<Long, List<Int>>, metricName: String, testUUID: UUID) {
        val all = data.map { (k, v) ->  k to v[0] }.toMap()
        val ok = data.map { (k, v) ->  k to v[1] }.toMap()
        val ko = data.map { (k, v) ->  k to v[2] }.toMap()
        val lineProtocolAll = all.map { (timestamp, value) ->
            "$metricName,flavor=all,testUUID=$testUUID value=${value.toDouble()} ${timestamp * 1000}"
        }.joinToString("\n")
        val lineProtocolOk = ok.map { (timestamp, value) ->
            "$metricName,flavor=ok,testUUID=$testUUID value=${value.toDouble()} ${timestamp * 1000}"
        }.joinToString("\n")
        val lineProtocolKo = ko.map { (timestamp, value) ->
            "$metricName,flavor=ko,testUUID=$testUUID value=${value.toDouble()} ${timestamp * 1000}"
        }.joinToString("\n")
        postToInflux(lineProtocolAll)
        postToInflux(lineProtocolOk)
        postToInflux(lineProtocolKo)
    }

    private suspend fun postToInflux(lineProtocol: String) {
        val url = "http://localhost:8086/api/v2/write?org=misarch&bucket=gatling&precision=ms"
        webClient.post()
            .uri(url)
            .header("Authorization", "Token my-secret-token")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue(lineProtocol)
            .retrieve()
            .awaitBodilessEntity()
    }
}