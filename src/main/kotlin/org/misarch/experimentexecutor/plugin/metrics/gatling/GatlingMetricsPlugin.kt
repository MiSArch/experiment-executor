package org.misarch.experimentexecutor.plugin.metrics.gatling

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge
import io.prometheus.client.exporter.PushGateway
import org.misarch.experimentexecutor.plugin.metrics.MetricPluginInterface
import org.misarch.experimentexecutor.plugin.metrics.gatling.model.GatlingStats
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

class GatlingMetricsPlugin(
    private val webClient: WebClient,
    private val influxUrl: String,
    private val pushGatewayUrl: String,
) : MetricPluginInterface {

    // TODO export Metrics also with a 0 timestamp for comparability
    // TODO post per request gauges to InfluxDB
    // TODO remove prometheus pushgateway

    override suspend fun exportMetrics(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        gatlingStatsJs: String,
        gatlingStatsHtml: String,
    ) {

        val responseTimeStats = parseGatlingResponseTimeStats(gatlingStatsJs)

        postResponseTimeStatsToInflux(responseTimeStats, testUUID, testVersion, Instant.now().toEpochMilli())
        postResponseTimeStatsToInflux(responseTimeStats, testUUID, testVersion, 946684800000) // 2000-01-01T00:00:00Z

        val registry = CollectorRegistry.defaultRegistry
        registry.clear()

        registry.registerResponseTimeGauges(responseTimeStats)
        responseTimeStats.contents?.forEach { (request, requestStats) ->
            registry.registerResponseTimeGauges(requestStats, suffix = "_${request.split("-").first().replace("-", "_")}")
        }

        // TODO responsetimepercentilesovertimeokPercentiles -> list of maps with timestamp and list which represent the response time percentiles at the timepoint
        val dataSeries = extractDataSeries(gatlingStatsHtml)
        dataSeries.forEach { (series, data) ->
            if (series == "requests" || series == "responses") {
                postTotalOkKoSeriesToInflux(data, series, testUUID, testVersion)
            }
        }

        val activeUsers = extractActiveUsers(gatlingStatsHtml)
        activeUsers.forEach { (scenario, data) -> postActiveUsersToInflux(data, scenario, "activeUsers", testUUID, testVersion) }

        logger.info { "🚀 Gatling Metrics pushed to InfluxDB" }

        val pushGateway = PushGateway(pushGatewayUrl)
        pushGateway.pushAdd(registry, "gatling_metrics", mapOf("testUUID" to testUUID.toString(), "testVersion" to testVersion))

        logger.info { "🚀 Gatling Metrics pushed to Prometheus Pushgateway" }
    }

    private fun parseGatlingResponseTimeStats(gatlingStatsJs: String): GatlingStats {
        val json = gatlingStatsJs.trimGatlingStatsJs()
        return ObjectMapper().readValue(json, GatlingStats::class.java)
    }

    private fun extractDataSeries(gatlingStatsHtml: String): Map<String, Map<Long, List<Int>>> {

        val dataSeries = mutableMapOf<String, Map<Long, List<Int>>>()
        val regex = """(?s)var (\w+)\s*=\s*unpack\(\s*(\[.*?])\s*\);""".toRegex()

        regex.findAll(gatlingStatsHtml).forEach { matchResult ->
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

    private fun extractActiveUsers(gatlingStatsHtml: String): Map<String, List<Pair<Long, Int>>> {

        val regex = Regex(
            """name:\s*'([^']+)'\s*,\s*data:\s*\[((?:\s*\[[^\[\]]+],?\s*)+)]""",
            RegexOption.DOT_MATCHES_ALL
        )

        return regex.findAll(gatlingStatsHtml).map { matchResult ->
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

        stats.stats.numberOfRequests.total.toDoubleOrNull()
            ?.let { register("gatling_number_of_requests_total$suffix", "Total number of requests").set(it) }
        stats.stats.numberOfRequests.ok.toDoubleOrNull()
            ?.let { register("gatling_number_of_requests_ok$suffix", "Number of successful requests").set(it) }
        stats.stats.numberOfRequests.ko.toDoubleOrNull()
            ?.let { register("gatling_number_of_requests_ko$suffix", "Number of failed requests").set(it) }

        stats.stats.meanResponseTime.total.toDoubleOrNull()?.let { register("gatling_mean_response_time$suffix", "Mean response time").set(it) }
        stats.stats.meanResponseTime.ok.toDoubleOrNull()
            ?.let { register("gatling_mean_response_time_ok$suffix", "Mean response time for successful requests").set(it) }
        stats.stats.meanResponseTime.ko.toDoubleOrNull()
            ?.let { register("gatling_mean_response_time_ko$suffix", "Mean response time for failed requests").set(it) }

        stats.stats.minResponseTime.total.toDoubleOrNull()?.let { register("gatling_min_response_time$suffix", "Min response time").set(it) }
        stats.stats.minResponseTime.ok.toDoubleOrNull()
            ?.let { register("gatling_min_response_time_ok$suffix", "Min response time for successful requests").set(it) }
        stats.stats.minResponseTime.ko.toDoubleOrNull()
            ?.let { register("gatling_min_response_time_ko$suffix", "Min response time for failed requests").set(it) }

        stats.stats.maxResponseTime.total.toDoubleOrNull()?.let { register("gatling_max_response_time$suffix", "Max response time").set(it) }
        stats.stats.maxResponseTime.ok.toDoubleOrNull()
            ?.let { register("gatling_max_response_time_ok$suffix", "Max response time for successful requests").set(it) }
        stats.stats.maxResponseTime.ko.toDoubleOrNull()
            ?.let { register("gatling_max_response_time_ko$suffix", "Max response time for failed requests").set(it) }

        stats.stats.standardDeviation.total.toDoubleOrNull()
            ?.let { register("gatling_standard_deviation$suffix", "Standard deviation of response time").set(it) }
        stats.stats.standardDeviation.ok.toDoubleOrNull()
            ?.let { register("gatling_standard_deviation_ok$suffix", "Standard deviation for successful requests").set(it) }
        stats.stats.standardDeviation.ko.toDoubleOrNull()
            ?.let { register("gatling_standard_deviation_ko$suffix", "Standard deviation for failed requests").set(it) }

        stats.stats.meanNumberOfRequestsPerSecond.total.toDoubleOrNull()
            ?.let { register("gatling_mean_requests_per_second$suffix", "Mean number of requests per second").set(it) }
        stats.stats.meanNumberOfRequestsPerSecond.ok.toDoubleOrNull()
            ?.let { register("gatling_mean_requests_per_second_ok$suffix", "Mean number of successful requests per second").set(it) }
        stats.stats.meanNumberOfRequestsPerSecond.ko.toDoubleOrNull()
            ?.let { register("gatling_mean_requests_per_second_ko$suffix", "Mean number of failed requests per second").set(it) }

        // Percentiles
        stats.stats.percentiles1.total.toDoubleOrNull()?.let { register("gatling_percentiles1$suffix", "Percentiles 1").set(it) }
        stats.stats.percentiles1.ok.toDoubleOrNull()
            ?.let { register("gatling_percentiles1_ok$suffix", "Percentiles 1 for successful requests").set(it) }
        stats.stats.percentiles1.ko.toDoubleOrNull()?.let { register("gatling_percentiles1_ko$suffix", "Percentiles 1 for failed requests").set(it) }

        stats.stats.percentiles2.total.toDoubleOrNull()?.let { register("gatling_percentiles2$suffix", "Percentiles 2").set(it) }
        stats.stats.percentiles2.ok.toDoubleOrNull()
            ?.let { register("gatling_percentiles2_ok$suffix", "Percentiles 2 for successful requests").set(it) }
        stats.stats.percentiles2.ko.toDoubleOrNull()?.let { register("gatling_percentiles2_ko$suffix", "Percentiles 2 for failed requests").set(it) }

        stats.stats.percentiles3.total.toDoubleOrNull()?.let { register("gatling_percentiles3$suffix", "Percentiles 3").set(it) }
        stats.stats.percentiles3.ok.toDoubleOrNull()
            ?.let { register("gatling_percentiles3_ok$suffix", "Percentiles 3 for successful requests").set(it) }
        stats.stats.percentiles3.ko.toDoubleOrNull()?.let { register("gatling_percentiles3_ko$suffix", "Percentiles 3 for failed requests").set(it) }

        stats.stats.percentiles4.total.toDoubleOrNull()?.let { register("gatling_percentiles4$suffix", "Percentiles 4").set(it) }
        stats.stats.percentiles4.ok.toDoubleOrNull()
            ?.let { register("gatling_percentiles4_ok$suffix", "Percentiles 4 for successful requests").set(it) }
        stats.stats.percentiles4.ko.toDoubleOrNull()?.let { register("gatling_percentiles4_ko$suffix", "Percentiles 4 for failed requests").set(it) }

        // Groups
        register("gatling_group1_count$suffix", "Group 1 count").set(stats.stats.group1.count.toDouble())
        register("gatling_group2_count$suffix", "Group 2 count").set(stats.stats.group2.count.toDouble())
        register("gatling_group3_count$suffix", "Group 3 count").set(stats.stats.group3.count.toDouble())
        register("gatling_group4_count$suffix", "Group 4 count").set(stats.stats.group4.count.toDouble())
    }

    private fun CollectorRegistry.register(name: String, help: String) =
        Gauge.build().name(name).help(help).register(this)

    private suspend fun postActiveUsersToInflux(data: List<Pair<Long, Int>>, scenario: String, metricName: String, testUUID: UUID, testVersion: String) {
        val lineProtocol = data.joinToString("\n") { (timestamp, value) ->
            "$metricName,scenario=${scenario.replace(" ", "")},testUUID=$testUUID,testVersion=$testVersion value=${value.toDouble()} $timestamp"
        }
        postToInflux(lineProtocol)
    }

    private suspend fun postTotalOkKoSeriesToInflux(data: Map<Long, List<Int>>, metricName: String, testUUID: UUID, testVersion: String) {
        val all = data.map { (k, v) -> k to v[0] }.toMap()
        val ok = data.map { (k, v) -> k to v[1] }.toMap()
        val ko = data.map { (k, v) -> k to v[2] }.toMap()
        val lineProtocolAll = all.map { (timestamp, value) ->
            "$metricName,flavor=all,testUUID=$testUUID,testVersion=$testVersion value=${value.toDouble()} ${timestamp * 1000}"
        }.joinToString("\n")
        val lineProtocolOk = ok.map { (timestamp, value) ->
            "$metricName,flavor=ok,testUUID=$testUUID,testVersion=$testVersion value=${value.toDouble()} ${timestamp * 1000}"
        }.joinToString("\n")
        val lineProtocolKo = ko.map { (timestamp, value) ->
            "$metricName,flavor=ko,testUUID=$testUUID,testVersion=$testVersion value=${value.toDouble()} ${timestamp * 1000}"
        }.joinToString("\n")
        postToInflux(lineProtocolAll)
        postToInflux(lineProtocolOk)
        postToInflux(lineProtocolKo)
    }

    private suspend fun postResponseTimeStatsToInflux(responseTimeStats: GatlingStats, testUUID: UUID, testVersion: String, epochTimestamp: Long) {
        val metrics = mapOf(
            "meanResponseTime" to responseTimeStats.stats.meanResponseTime.total.toDoubleOrNull(),
            "meanResponseTimeOk" to responseTimeStats.stats.meanResponseTime.ok.toDoubleOrNull(),
            "meanResponseTimeKo" to responseTimeStats.stats.meanResponseTime.ko.toDoubleOrNull(),
            "minResponseTime" to responseTimeStats.stats.minResponseTime.total.toDoubleOrNull(),
            "minResponseTimeOk" to responseTimeStats.stats.minResponseTime.ok.toDoubleOrNull(),
            "minResponseTimeKo" to responseTimeStats.stats.minResponseTime.ko.toDoubleOrNull(),
            "maxResponseTime" to responseTimeStats.stats.maxResponseTime.total.toDoubleOrNull(),
            "maxResponseTimeOk" to responseTimeStats.stats.maxResponseTime.ok.toDoubleOrNull(),
            "maxResponseTimeKo" to responseTimeStats.stats.maxResponseTime.ko.toDoubleOrNull(),
            "standardDeviation" to responseTimeStats.stats.standardDeviation.total.toDoubleOrNull(),
            "standardDeviationOk" to responseTimeStats.stats.standardDeviation.ok.toDoubleOrNull(),
            "standardDeviationKo" to responseTimeStats.stats.standardDeviation.ko.toDoubleOrNull(),
            "percentiles1" to responseTimeStats.stats.percentiles1.total.toDoubleOrNull(),
            "percentiles1Ok" to responseTimeStats.stats.percentiles1.ok.toDoubleOrNull(),
            "percentiles1Ko" to responseTimeStats.stats.percentiles1.ko.toDoubleOrNull(),
            "percentiles2" to responseTimeStats.stats.percentiles2.total.toDoubleOrNull(),
            "percentiles2Ok" to responseTimeStats.stats.percentiles2.ok.toDoubleOrNull(),
            "percentiles2Ko" to responseTimeStats.stats.percentiles2.ko.toDoubleOrNull(),
            "percentiles3" to responseTimeStats.stats.percentiles3.total.toDoubleOrNull(),
            "percentiles3Ok" to responseTimeStats.stats.percentiles3.ok.toDoubleOrNull(),
            "percentiles3Ko" to responseTimeStats.stats.percentiles3.ko.toDoubleOrNull(),
            "percentiles4" to responseTimeStats.stats.percentiles4.total.toDoubleOrNull(),
            "percentiles4Ok" to responseTimeStats.stats.percentiles4.ok.toDoubleOrNull(),
            "percentiles4Ko" to responseTimeStats.stats.percentiles4.ko.toDoubleOrNull(),
            "numberOfRequestsTotal" to responseTimeStats.stats.numberOfRequests.total.toDoubleOrNull(),
            "numberOfRequestsOk" to responseTimeStats.stats.numberOfRequests.ok.toDoubleOrNull(),
            "numberOfRequestsKo" to responseTimeStats.stats.numberOfRequests.ko.toDoubleOrNull(),
            "meanNumberOfRequestsPerSecondTotal" to responseTimeStats.stats.meanNumberOfRequestsPerSecond.total.toDoubleOrNull(),
            "meanNumberOfRequestsPerSecondOk" to responseTimeStats.stats.meanNumberOfRequestsPerSecond.ok.toDoubleOrNull(),
            "meanNumberOfRequestsPerSecondKo" to responseTimeStats.stats.meanNumberOfRequestsPerSecond.ko.toDoubleOrNull(),
            "group1Count" to responseTimeStats.stats.group1.count.toDouble(),
            "group2Count" to responseTimeStats.stats.group2.count.toDouble(),
            "group3Count" to responseTimeStats.stats.group3.count.toDouble(),
            "group4Count" to responseTimeStats.stats.group4.count.toDouble()
        )
        metrics.forEach { (metricName, value) ->
            if (value != null) {
                val lineProtocol = "$metricName,testUUID=$testUUID,testVersion=$testVersion value=$value $epochTimestamp"
                postToInflux(lineProtocol)
            }
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