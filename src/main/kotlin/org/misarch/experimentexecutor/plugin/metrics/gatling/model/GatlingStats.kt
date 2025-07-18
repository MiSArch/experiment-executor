package org.misarch.experimentexecutor.plugin.metrics.gatling.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class GatlingStats(
    @JsonProperty("type") val type: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("path") val path: String,
    @JsonProperty("pathFormatted") val pathFormatted: String,
    @JsonProperty("stats") val stats: Stats,
    @JsonProperty("contents") val contents: Map<String, GatlingStats>?,
) {
    data class Stats(
        @JsonProperty("name") val name: String,
        @JsonProperty("numberOfRequests") val numberOfRequests: TotalOkKo,
        @JsonProperty("minResponseTime") val minResponseTime: TotalOkKo,
        @JsonProperty("maxResponseTime") val maxResponseTime: TotalOkKo,
        @JsonProperty("meanResponseTime") val meanResponseTime: TotalOkKo,
        @JsonProperty("standardDeviation") val standardDeviation: TotalOkKo,
        @JsonProperty("percentiles1") val percentiles1: TotalOkKo,
        @JsonProperty("percentiles2") val percentiles2: TotalOkKo,
        @JsonProperty("percentiles3") val percentiles3: TotalOkKo,
        @JsonProperty("percentiles4") val percentiles4: TotalOkKo,
        @JsonProperty("group1") val group1: NameHtmlCountPercentage,
        @JsonProperty("group2") val group2: NameHtmlCountPercentage,
        @JsonProperty("group3") val group3: NameHtmlCountPercentage,
        @JsonProperty("group4") val group4: NameHtmlCountPercentage,
        @JsonProperty("meanNumberOfRequestsPerSecond") val meanNumberOfRequestsPerSecond: TotalOkKo,
    ) {
        @get:JsonIgnore
        val percentageMeanNumberOfRequestsPerSecond: TotalOkKo
            get() =
                TotalOkKo(
                    total = "1.0",
                    ok = calculatePercentageMeanNumberOfRequestsPerSecondOk()?.toString() ?: "0.0",
                    ko = calculatePercentageMeanNumberOfRequestsPerSecondKo()?.toString() ?: "0.0",
                )
    }
}

data class TotalOkKo(
    @JsonProperty("total") val total: String,
    @JsonProperty("ok") val ok: String,
    @JsonProperty("ko") val ko: String,
)

data class NameHtmlCountPercentage(
    @JsonProperty("name") val name: String,
    @JsonProperty("htmlName") val htmlName: String,
    @JsonProperty("count") val count: Int,
    @JsonProperty("percentage") val percentage: Double,
)

fun GatlingStats.Stats.calculatePercentageMeanNumberOfRequestsPerSecondOk(): Double? {
    val total = meanNumberOfRequestsPerSecond.total.toDoubleOrNull() ?: return null
    val ok = meanNumberOfRequestsPerSecond.ok.toDoubleOrNull() ?: return null
    return if (total == 0.0) 0.0 else (ok / total) * 100
}

fun GatlingStats.Stats.calculatePercentageMeanNumberOfRequestsPerSecondKo(): Double? {
    val total = meanNumberOfRequestsPerSecond.total.toDoubleOrNull() ?: return null
    val ko = meanNumberOfRequestsPerSecond.ko.toDoubleOrNull() ?: return null
    return if (total == 0.0) 0.0 else (ko / total) * 100
}
