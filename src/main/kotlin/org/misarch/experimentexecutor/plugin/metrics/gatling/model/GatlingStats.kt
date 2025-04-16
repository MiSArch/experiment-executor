package org.misarch.experimentexecutor.plugin.metrics.gatling.model

data class GatlingStats(
    val type: String,
    val name: String,
    val path: String,
    val pathFormatted: String,
    val stats: Stats,
    val contents: Map<String, GatlingStats> = emptyMap()
) {
    data class Stats(
        val name: String,
        val numberOfRequests: TotalOkKo,
        val minResponseTime: TotalOkKo,
        val maxResponseTime: TotalOkKo,
        val meanResponseTime: TotalOkKo,
        val standardDeviation: TotalOkKo,
        val percentiles1: TotalOkKo,
        val percentiles2: TotalOkKo,
        val percentiles3: TotalOkKo,
        val percentiles4: TotalOkKo,
        val group1: NameHtmlCountPercentage,
        val group2: NameHtmlCountPercentage,
        val group3: NameHtmlCountPercentage,
        val group4: NameHtmlCountPercentage,
        val meanNumberOfRequestsPerSecond: TotalOkKo,
    )
}

data class TotalOkKo(
    val total: Int,
    val ok: Int,
    val ko: Int,
)

data class NameHtmlCountPercentage(
    val name: String,
    val htmlName: String,
    val count: Int,
    val percentage: Double
)