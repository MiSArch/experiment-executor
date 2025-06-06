package org.misarch.experimentexecutor.plugin.metrics.gatling

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.misarch.experimentexecutor.plugin.metrics.MetricPluginInterface
import org.misarch.experimentexecutor.plugin.metrics.gatling.model.GatlingStats
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

private val logger = KotlinLogging.logger {}

class GatlingMetricsPlugin(
    private val webClient: WebClient,
    private val influxUrl: String,
    private val influxToken: String,
    private val storeResultDataInFiles: Boolean,
    private val basePath: String,
) : MetricPluginInterface {

    override suspend fun exportMetrics(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        gatlingStatsJs: String,
        gatlingStatsHtml: String,
    ) {

        if (storeResultDataInFiles) {
            storeResultDataInFiles(testUUID, testVersion, startTime, gatlingStatsJs, gatlingStatsHtml)
        }

        // Response Time Metrics
        val responseTimeStats = parseGatlingResponseTimeStats(gatlingStatsJs)
        postResponseTimeStatsToInflux(responseTimeStats, testUUID, testVersion, endTime.minusSeconds(60))

        // TODO responsetimepercentilesovertimeokPercentiles -> list of maps with timestamp and list which represent the response time percentiles at the timepoint
        // Requests and Responses over Time
        val dataSeries = extractDataSeries(gatlingStatsHtml)
        dataSeries.forEach { (series, data) ->
            if (series == "requests" || series == "responses") {
                postTotalOkKoSeriesToInflux(data, series, testUUID, testVersion)
            }
        }

        // Active Users Over Time
        val activeUsers = extractActiveUsers(gatlingStatsHtml)
        activeUsers.forEach { (scenario, data) -> postActiveUsersToInflux(data, scenario, "activeUsers", testUUID, testVersion) }

        logger.info { "🚀 Gatling Metrics pushed to InfluxDB" }
    }

    private fun storeResultDataInFiles(testUUID: UUID, testVersion: String, startTime: Instant, gatlingStatsJs: String, gatlingStatsHtml: String) {
        val folderName = startTime.truncatedTo(ChronoUnit.SECONDS).toString().replace(":", "-")
        val reportDir = File("$basePath/$testUUID/$testVersion/reports/gatling/$folderName").apply {
            mkdirs()
        }

        File(reportDir, "stats.js").writeText(gatlingStatsJs)
        File(reportDir, "index.html").writeText(gatlingStatsHtml)
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

    private suspend fun postActiveUsersToInflux(
        data: List<Pair<Long, Int>>,
        scenario: String,
        metricName: String,
        testUUID: UUID,
        testVersion: String
    ) {
        val lineProtocol = data.joinToString("\n") { (timestamp, value) ->
            "$metricName,scenario=${scenario.replace(" ", "")},testUUID=$testUUID,testVersion=$testVersion value=${value.toDouble()} $timestamp"
        }
        postToInflux(lineProtocol)
    }

    private suspend fun postTotalOkKoSeriesToInflux(data: Map<Long, List<Int>>, metricName: String, testUUID: UUID, testVersion: String) {
        val all = data.map { (k, v) -> k to v[0] }.toMap()
        val ok = data.map { (k, v) -> k to v[1] }.toMap()
        val ko = data.map { (k, v) -> k to v[2] }.toMap()
        postToInflux(all.toLineProtocolOkKo(metricName, testUUID, testVersion, "all"))
        postToInflux(ok.toLineProtocolOkKo(metricName, testUUID, testVersion, "ok"))
        postToInflux(ko.toLineProtocolOkKo(metricName, testUUID, testVersion, "ko"))
    }

    private fun Map<Long, Int>.toLineProtocolOkKo(metricName: String, testUUID: UUID, testVersion: String, flavor: String) =
        map { (timestamp, value) ->
            "$metricName,flavor=$flavor,testUUID=$testUUID,testVersion=$testVersion value=${value.toDouble()} ${timestamp * 1000}"
        }.joinToString("\n")


    private suspend fun postResponseTimeStatsToInflux(
        responseTimeStats: GatlingStats,
        testUUID: UUID,
        testVersion: String,
        endTime: Instant,
    ) {

        val metrics = responseTimeStats.buildGatlingGauges()

        metrics.forEach { (metricName, value) ->
            if (value != null) {
                val lineProtocol = "$metricName,testUUID=$testUUID,testVersion=$testVersion value=$value ${endTime.toEpochMilli()}"
                postToInflux(lineProtocol)
            }
        }

        val metricsPerRequest = responseTimeStats.contents?.map { (requestName, stats) ->
            stats.buildGatlingGauges(suffix = "_${requestName.split("-").first().replace("-", "_").removePrefix("req_")}")
        }

        metricsPerRequest?.forEach { request ->
            request.forEach { (metricName, value) ->
                if (value != null) {
                    val lineProtocol = "$metricName,testUUID=$testUUID,testVersion=$testVersion value=$value ${endTime.toEpochMilli()}"
                    postToInflux(lineProtocol)
                }
            }
        }
    }

    private fun GatlingStats.buildGatlingGauges(suffix: String = "") = mapOf(
        "meanResponseTime$suffix" to stats.meanResponseTime.total.toDoubleOrNull(),
        "meanResponseTimeOk$suffix" to stats.meanResponseTime.ok.toDoubleOrNull(),
        "meanResponseTimeKo$suffix" to stats.meanResponseTime.ko.toDoubleOrNull(),
        "minResponseTime$suffix" to stats.minResponseTime.total.toDoubleOrNull(),
        "minResponseTimeOk$suffix" to stats.minResponseTime.ok.toDoubleOrNull(),
        "minResponseTimeKo$suffix" to stats.minResponseTime.ko.toDoubleOrNull(),
        "maxResponseTime$suffix" to stats.maxResponseTime.total.toDoubleOrNull(),
        "maxResponseTimeOk$suffix" to stats.maxResponseTime.ok.toDoubleOrNull(),
        "maxResponseTimeKo$suffix" to stats.maxResponseTime.ko.toDoubleOrNull(),
        "standardDeviation$suffix" to stats.standardDeviation.total.toDoubleOrNull(),
        "standardDeviationOk$suffix" to stats.standardDeviation.ok.toDoubleOrNull(),
        "standardDeviationKo$suffix" to stats.standardDeviation.ko.toDoubleOrNull(),
        "percentiles1$suffix" to stats.percentiles1.total.toDoubleOrNull(),
        "percentiles1Ok$suffix" to stats.percentiles1.ok.toDoubleOrNull(),
        "percentiles1Ko$suffix" to stats.percentiles1.ko.toDoubleOrNull(),
        "percentiles2$suffix" to stats.percentiles2.total.toDoubleOrNull(),
        "percentiles2Ok$suffix" to stats.percentiles2.ok.toDoubleOrNull(),
        "percentiles2Ko$suffix" to stats.percentiles2.ko.toDoubleOrNull(),
        "percentiles3$suffix" to stats.percentiles3.total.toDoubleOrNull(),
        "percentiles3Ok$suffix" to stats.percentiles3.ok.toDoubleOrNull(),
        "percentiles3Ko$suffix" to stats.percentiles3.ko.toDoubleOrNull(),
        "percentiles4$suffix" to stats.percentiles4.total.toDoubleOrNull(),
        "percentiles4Ok$suffix" to stats.percentiles4.ok.toDoubleOrNull(),
        "percentiles4Ko$suffix" to stats.percentiles4.ko.toDoubleOrNull(),
        "numberOfRequestsTotal$suffix" to stats.numberOfRequests.total.toDoubleOrNull(),
        "numberOfRequestsOk$suffix" to stats.numberOfRequests.ok.toDoubleOrNull(),
        "numberOfRequestsKo$suffix" to stats.numberOfRequests.ko.toDoubleOrNull(),
        "meanNumberOfRequestsPerSecondTotal$suffix" to stats.meanNumberOfRequestsPerSecond.total.toDoubleOrNull(),
        "meanNumberOfRequestsPerSecondOk$suffix" to stats.meanNumberOfRequestsPerSecond.ok.toDoubleOrNull(),
        "meanNumberOfRequestsPerSecondKo$suffix" to stats.meanNumberOfRequestsPerSecond.ko.toDoubleOrNull(),
        "group1Count$suffix" to stats.group1.count.toDouble(),
        "group2Count$suffix" to stats.group2.count.toDouble(),
        "group3Count$suffix" to stats.group3.count.toDouble(),
        "group4Count$suffix" to stats.group4.count.toDouble()
    )

    private suspend fun postToInflux(lineProtocol: String) {
        webClient.post()
            .uri(influxUrl)
            .header("Authorization", "Token $influxToken")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue(lineProtocol)
            .retrieve()
            .awaitBodilessEntity()
    }
}