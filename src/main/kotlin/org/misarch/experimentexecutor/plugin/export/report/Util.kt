package org.misarch.experimentexecutor.plugin.export.report

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.delay
import org.misarch.experimentexecutor.config.GATLING_RESPONSE_TIME_STATS_FILENAME
import org.misarch.experimentexecutor.model.Goal
import org.misarch.experimentexecutor.plugin.export.report.model.GoalViolation
import org.misarch.experimentexecutor.plugin.metrics.gatling.model.GatlingStats
import org.misarch.experimentexecutor.plugin.metrics.gatling.parseGatlingResponseTimeStats
import java.io.File

suspend fun checkGoalViolations(
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
            ).toFloat()

        if (goal.threshold.toFloat() < value) {
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

fun extractGoals(
    gatlingStatsJs: String,
    factor: Float,
): List<Goal> {
    val parsedStats = parseGatlingResponseTimeStats(gatlingStatsJs)
    return labelToStatsAttribute
        .mapNotNull { (metric, statsAttribute) ->
            val value =
                parsedStats.stats::class
                    .members
                    .firstOrNull { it.name == statsAttribute.substringBefore('.') }
                    ?.call(parsedStats.stats)
                    ?.let { field ->
                        val subField = statsAttribute.substringAfter('.', "")
                        if (subField.isNotEmpty()) {
                            val finalValue = field::class.members.firstOrNull { it.name == subField }?.call(field)
                            (finalValue as? String)?.toDoubleOrNull() ?: (finalValue as? Double) ?: 0.0
                        } else {
                            (field as? String)?.toDoubleOrNull() ?: (field as? Double) ?: 0.0
                        }
                    }

            value?.let {
                val f = if (metric.startsWith("percentage")) 1.0F else factor
                Goal(
                    metric = metric,
                    threshold = (it * f).toString(),
                    color = "red",
                )
            }
        }
}

val labelToStatsAttribute =
    mapOf(
        "percentage reqs with resp. time t < 800 ms" to "group1.percentage",
        "percentage reqs with resp. time 800 ms < t < 1200 ms" to "group2.percentage",
        "percentage reqs with resp. time t > 1200 ms" to "group3.percentage",
        "percentage failed requests" to "group4.percentage",
        "percentage mean requests/sec ok" to "percentageMeanNumberOfRequestsPerSecond.ok",
        "percentage mean requests/sec ko" to "percentageMeanNumberOfRequestsPerSecond.ko",
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
