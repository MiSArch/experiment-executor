package org.misarch.experimentexecutor.plugin.metrics

import java.time.Instant
import java.util.*

interface MetricPluginInterface {
    suspend fun exportMetrics(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        gatlingStatsJs: String,
        gatlingStatsHtml: String,
    )
}
