package org.misarch.experimentexecutor.plugin.metrics.gatling

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
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
) : MetricPluginInterface {

    companion object {
        const val EPOCH_TIMESTAMP_SECONDS = 946684800L  // 2000-01-01T00:00:00Z
    }

    override suspend fun exportMetrics(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        gatlingStatsJs: String,
        gatlingStatsHtml: String,
    ) {

        // Response Time Metrics
        val responseTimeStats = parseGatlingResponseTimeStats(gatlingStatsJs)
        postResponseTimeStatsToInflux(responseTimeStats, testUUID, testVersion, Instant.now().toEpochMilli())
        postResponseTimeStatsToInflux(responseTimeStats, testUUID, testVersion, EPOCH_TIMESTAMP_SECONDS * 1000)

        // TODO responsetimepercentilesovertimeokPercentiles -> list of maps with timestamp and list which represent the response time percentiles at the timepoint
        // Requests and Responses over Time
        val dataSeries = extractDataSeries(gatlingStatsHtml)
        dataSeries.forEach { (series, data) ->
            if (series == "requests" || series == "responses") {
                postTotalOkKoSeriesToInflux(data, series, testUUID, testVersion)
                postTotalOkSeriesToInfluxWithNullTime(data, series, testUUID, testVersion)
            }
        }

        // Active Users Over Time
        val activeUsers = extractActiveUsers(gatlingStatsHtml)
        val activeUsersWithNullTime = activeUsers.transformToNullTimestamp()
        activeUsers.forEach { (scenario, data) -> postActiveUsersToInflux(data, scenario, "activeUsers", testUUID, testVersion) }
        activeUsersWithNullTime.forEach { (scenario, data) -> postActiveUsersToInflux(data, scenario, "activeUsers", testUUID, testVersion) }

        logger.info { "🚀 Gatling Metrics pushed to InfluxDB" }
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

    private fun Map<String, List<Pair<Long, Int>>>.transformToNullTimestamp(): Map<String, List<Pair<Long, Int>>> {
        return mapValues { (_, data) ->
            data.mapIndexed { i, pair ->
                Pair((EPOCH_TIMESTAMP_SECONDS + i + 1) * 1000, pair.second)
            }
        }
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

    private suspend fun postTotalOkSeriesToInfluxWithNullTime(data: Map<Long, List<Int>>, metricName: String, testUUID: UUID, testVersion: String) {
        val all: MutableMap<Long, Int> = mutableMapOf()
        val ok: MutableMap<Long, Int> = mutableMapOf()
        val ko: MutableMap<Long, Int> = mutableMapOf()
        data.toList().mapIndexed { i, (_, v) ->
            all[EPOCH_TIMESTAMP_SECONDS + i + 1] = v[0]
            ok[EPOCH_TIMESTAMP_SECONDS + i + 1] = v[1]
            ko[EPOCH_TIMESTAMP_SECONDS + i + 1] = v[2]
        }
        postToInflux(all.toLineProtocolOkKo(metricName, testUUID, testVersion, "all"))
        postToInflux(ok.toLineProtocolOkKo(metricName, testUUID, testVersion, "ok"))
        postToInflux(ko.toLineProtocolOkKo(metricName, testUUID, testVersion, "ko"))
    }

    private fun Map<Long, Int>.toLineProtocolOkKo(metricName: String, testUUID: UUID, testVersion: String, flavor: String) =
        map { (timestamp, value) ->
            "$metricName,flavor=$flavor,testUUID=$testUUID,testVersion=$testVersion value=${value.toDouble()} ${timestamp * 1000}"
        }.joinToString("\n")


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