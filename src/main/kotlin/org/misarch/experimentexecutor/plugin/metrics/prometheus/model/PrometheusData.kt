package org.misarch.experimentexecutor.plugin.metrics.prometheus.model

data class PrometheusResponse(
    val status: String,
    val data: PrometheusData
)

data class PrometheusData(
    val resultType: String,
    val result: List<PrometheusMetric>
)

data class PrometheusMetric(
    val metric: Map<String, String>,
    val values: List<List<String>>
)
