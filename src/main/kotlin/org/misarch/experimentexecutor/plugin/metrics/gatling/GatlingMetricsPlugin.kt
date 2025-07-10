package org.misarch.experimentexecutor.plugin.metrics.gatling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.misarch.experimentexecutor.config.GATLING_RESPONSE_TIME_STATS_FILENAME
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
        // Response Time Metrics
        val responseTimeStats = parseGatlingResponseTimeStats(gatlingStatsJs)
        postResponseTimeStatsToInflux(responseTimeStats, testUUID, testVersion, endTime.minusSeconds(60))

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

        // responsetimepercentilesovertimeokPercentiles
        // -> list of maps with timestamp and list which represent the response time percentiles at the timepoint
        // -> could be added as well here

        storeResultDataInFiles(
            testUUID,
            testVersion,
            startTime,
            gatlingStatsJs,
            gatlingStatsHtml,
            responseTimeStats,
            dataSeries,
            activeUsers,
        )

        logger.info { "🚀 Gatling Metrics pushed to InfluxDB" }
    }

    private fun storeResultDataInFiles(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        gatlingStatsJs: String,
        gatlingStatsHtml: String,
        responseTimeStats: GatlingStats,
        dataSeries: Map<String, Map<Long, List<Int>>>,
        activeUsers: Map<String, List<Pair<Long, Int>>>,
    ) {
        val folderName = startTime.truncatedTo(ChronoUnit.SECONDS).toString().replace(":", "-")
        val reportDir =
            File("$basePath/$testUUID/$testVersion/reports/$folderName/gatling").apply {
                mkdirs()
            }

        File(reportDir, GATLING_RESPONSE_TIME_STATS_FILENAME).writeText(jacksonObjectMapper().writeValueAsString(responseTimeStats))
        if (storeResultDataInFiles) {
            File(reportDir, "dataSeries.json").writeText(jacksonObjectMapper().writeValueAsString(dataSeries))
            File(reportDir, "activeUsers.json").writeText(jacksonObjectMapper().writeValueAsString(activeUsers))
            File(reportDir, "stats.js").writeText(gatlingStatsJs)
            File(reportDir, "index.html").writeText(gatlingStatsHtml)
        }
    }

    private suspend fun postActiveUsersToInflux(
        data: List<Pair<Long, Int>>,
        scenario: String,
        metricName: String,
        testUUID: UUID,
        testVersion: String,
    ) {
        val lineProtocol =
            data.joinToString("\n") { (timestamp, value) ->
                "$metricName,scenario=${
                    scenario.replace(
                        " ",
                        "",
                    )
                },testUUID=$testUUID,testVersion=$testVersion value=${value.toDouble()} $timestamp"
            }
        postToInflux(lineProtocol)
    }

    private suspend fun postTotalOkKoSeriesToInflux(
        data: Map<Long, List<Int>>,
        metricName: String,
        testUUID: UUID,
        testVersion: String,
    ) {
        val all = data.map { (k, v) -> k to v[0] }.toMap()
        val ok = data.map { (k, v) -> k to v[1] }.toMap()
        val ko = data.map { (k, v) -> k to v[2] }.toMap()
        postToInflux(all.toLineProtocolOkKo(metricName, testUUID, testVersion, "all"))
        postToInflux(ok.toLineProtocolOkKo(metricName, testUUID, testVersion, "ok"))
        postToInflux(ko.toLineProtocolOkKo(metricName, testUUID, testVersion, "ko"))
    }

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

        val metricsPerRequest =
            responseTimeStats.contents?.map { (requestName, stats) ->
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

    private suspend fun postToInflux(lineProtocol: String) {
        webClient
            .post()
            .uri(influxUrl)
            .header("Authorization", "Token $influxToken")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue(lineProtocol)
            .retrieve()
            .awaitBodilessEntity()
    }
}
