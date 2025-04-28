package org.misarch.experimentexecutor.service.experiment

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.misarch.experimentexecutor.plugin.result.grafana.GrafanaPlugin
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class ExperimentResultService() {

    // TODO implement a plugin registry based on a configuration file
    val registry = listOf(
        GrafanaPlugin(),
    )

    suspend fun createAndExportReports(testUUID: UUID, startTime: Instant, endTime: Instant) {
        supervisorScope {
            registry.map { plugin ->
                async { plugin.createReport(testUUID, startTime, endTime) }
            }
        }.awaitAll()
    }
}