package org.misarch.experimentexecutor.plugin.metrics.gatling

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.misarch.experimentexecutor.model.Goal
import org.misarch.experimentexecutor.plugin.export.report.labelToStatsAttribute
import org.misarch.experimentexecutor.plugin.metrics.gatling.model.GatlingStats
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

fun parseGatlingResponseTimeStats(gatlingStatsJs: String): GatlingStats {
    val json = gatlingStatsJs.trimGatlingStatsJs()
    return ObjectMapper().readValue(json, GatlingStats::class.java)
}

fun String.trimGatlingStatsJs(): String =
    removePrefix("var stats = ")
        .split("function fillStats")
        .first()
        .replace("stats:", "\"stats\":")
        .replace("type:", "\"type\":")
        .replace("name:", "\"name\":")
        .replace("path:", "\"path\":")
        .replace("pathFormatted:", "\"pathFormatted\":")
        .replace("contents:", "\"contents\":")

fun extractDataSeries(gatlingStatsHtml: String): Map<String, Map<Long, List<Int>>> {
    val dataSeries = mutableMapOf<String, Map<Long, List<Int>>>()
    val regex = """(?s)var (\w+)\s*=\s*unpack\(\s*(\[.*?])\s*\);""".toRegex()

    regex.findAll(gatlingStatsHtml).forEach { matchResult ->
        val variableName = matchResult.groupValues[1]
        val arrayContents = matchResult.groupValues[2]

        val rawList: List<List<Any?>> = ObjectMapper().readValue(arrayContents, object : TypeReference<List<List<Any?>>>() {})

        val valueMap =
            rawList
                .associate { pair ->
                    val timestamp = (pair[0] as Number).toLong()
                    val values =
                        when (val second = pair[1]) {
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

fun extractActiveUsers(gatlingStatsHtml: String): Map<String, List<Pair<Long, Int>>> {
    val regex =
        Regex(
            """name:\s*'([^']+)'\s*,\s*data:\s*\[((?:\s*\[[^\[\]]+],?\s*)+)]""",
            RegexOption.DOT_MATCHES_ALL,
        )

    return regex
        .findAll(gatlingStatsHtml)
        .map { matchResult ->
            val name = matchResult.groupValues[1] // Extract the name
            val dataArray = "[${matchResult.groupValues[2]}]" // Extract the data array

            val rawData: List<List<Number>> =
                ObjectMapper().readValue(dataArray, object : TypeReference<List<List<Number>>>() {})

            val data = rawData.map { Pair(it[0].toLong(), it[1].toInt()) }

            Pair(name, data)
        }.toMap()
}

fun extractGoals(gatlingStatsJs: String): List<Goal> {
    val parsedStats = parseGatlingResponseTimeStats(gatlingStatsJs)
    return labelToStatsAttribute
        .filter { (metric, _) -> !metric.startsWith("number") }
        .mapNotNull { (metric, statsAttribute) ->
            val value =
                parsedStats.stats::class
                    .members
                    .firstOrNull { it.name == statsAttribute.substringBefore('.') }
                    ?.call(parsedStats.stats)
                    ?.let { field ->
                        val subField = statsAttribute.substringAfter('.', "")
                        if (subField.isNotEmpty()) {
                            field::class.members.firstOrNull { it.name == subField }?.call(field)
                        } else {
                            field
                        }
                    } as? String

            value?.let {
                Goal(
                    metric = metric,
                    threshold = ((it.toFloatOrNull() ?: 0.0F) * 1.5F).toInt().toString(),
                    color = "red",
                )
            }
        }.filter { it.threshold != "0" }
}

fun Map<Long, Int>.toLineProtocolOkKo(
    metricName: String,
    testUUID: UUID,
    testVersion: String,
    flavor: String,
) = map { (timestamp, value) ->
    "$metricName,flavor=$flavor,testUUID=$testUUID,testVersion=$testVersion value=${value.toDouble()} ${timestamp * 1000}"
}.joinToString("\n")

fun GatlingStats.buildGatlingGauges(suffix: String = "") =
    mapOf(
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
        "group4Count$suffix" to stats.group4.count.toDouble(),
    )
