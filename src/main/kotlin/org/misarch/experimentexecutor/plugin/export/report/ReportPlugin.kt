package org.misarch.experimentexecutor.plugin.export.report

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.delay
import org.misarch.experimentexecutor.config.GATLING_RESPONSE_TIME_STATS_FILENAME
import org.misarch.experimentexecutor.config.REPORT_FILENAME
import org.misarch.experimentexecutor.model.Goal
import org.misarch.experimentexecutor.plugin.export.ExportPluginInterface
import org.misarch.experimentexecutor.plugin.export.report.model.GoalViolation
import org.misarch.experimentexecutor.plugin.export.report.model.Report
import org.misarch.experimentexecutor.plugin.metrics.gatling.model.GatlingStats
import org.springframework.boot.info.BuildProperties
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class ReportPlugin(
    private val basePath: String,
    private val buildProperties: BuildProperties,
) : ExportPluginInterface {
    override suspend fun createReport(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        goals: List<Goal>,
        gatlingStatsHtml: String,
    ) {
        val folderName = startTime.truncatedTo(ChronoUnit.SECONDS).toString().replace(":", "-")
        val filePath = "$basePath/$testUUID/$testVersion/reports/$folderName"
        File(filePath).mkdirs()

        val report =
            Report(
                testUUID = testUUID.toString(),
                testVersion = testVersion,
                experimentExecutorVersion = buildProperties.version,
                startTime = startTime.toEpochMilli().toString(),
                endTime = endTime.toEpochMilli().toString(),
                goals = goals,
                goalViolations = checkGoalViolations(goals, filePath),
            )

        File("$filePath/$REPORT_FILENAME").writeText(jacksonObjectMapper().writeValueAsString(report))
    }

    private suspend fun checkGoalViolations(
        goals: List<Goal>,
        filePath: String,
    ): List<GoalViolation> {
        val statsFile = File("$filePath/gatling", GATLING_RESPONSE_TIME_STATS_FILENAME)
        while (!statsFile.exists()) {
            delay(100)
        }
        val stats = statsFile.readText().let { jacksonObjectMapper().readValue(it, GatlingStats::class.java) }
        return goals.mapNotNull { goal ->
            val statsAttribute = labelToStatsAttribute[goal.metric]
            val value =
                (
                    stats.stats::class
                        .members
                        .first { it.name == statsAttribute?.substringBefore('.') }
                        .call(stats.stats)
                        .let { field ->
                            val subField = statsAttribute?.substringAfter('.')
                            if (subField != null && field != null) {
                                field::class.members.first { it.name == subField }.call(field)
                            } else {
                                field
                            }
                        } as? String ?: return@mapNotNull null
                ).toInt()

            if (goal.threshold.toInt() < value) {
                GoalViolation(
                    metricName = goal.metric,
                    threshold = goal.threshold,
                    actualValue = value.toString(),
                )
            } else {
                null
            }
        }
    }

    val labelToStatsAttribute =
        mapOf(
            "number reqs with resp. time t < 800 ms" to "group1.count",
            "number reqs with resp. time t < 800 ms < t < 1200 ms" to "group2.count",
            "number reqs with resp. time t > 1200 ms" to "group3.count",
            "number failed requests" to "group4.count",
            "number of requests total" to "numberOfRequests.total",
            "number of requests total ok" to "numberOfRequests.ok",
            "number of requests total ko" to "numberOfRequests.ko",
            "mean requests/sec" to "meanNumberOfRequestsPerSecond.total",
            "mean requests/sec ok" to "meanNumberOfRequestsPerSecond.ok",
            "mean requests/sec ko" to "meanNumberOfRequestsPerSecond.ko",
            "min response time" to "minResponseTime.total",
            "mean response time" to "meanResponseTime.total",
            "max response time" to "maxResponseTime.total",
            "min response time ok" to "minResponseTime.ok",
            "mean response time ok" to "meanResponseTime.ok",
            "max response time ok" to "maxResponseTime.ok",
            "min response time ko" to "minResponseTime.ko",
            "mean response time ko" to "meanResponseTime.ko",
            "max response time ko" to "maxResponseTime.ko",
        )
}
