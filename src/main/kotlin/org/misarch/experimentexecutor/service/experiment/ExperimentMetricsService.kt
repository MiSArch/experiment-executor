package org.misarch.experimentexecutor.service.experiment

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.misarch.experimentexecutor.executor.model.WorkLoad
import org.misarch.experimentexecutor.plugin.metrics.gatling.GatlingMetricPlugin
import org.springframework.stereotype.Service
import java.util.*

@Service
class ExperimentMetricsService {

    // TODO implement a plugin registry based on a configuration file
    private val registry = listOf(
        GatlingMetricPlugin(),
    )

    suspend fun collectAndExportMetrics(workLoad: WorkLoad, testUUID: UUID) {
        supervisorScope {
            registry.map { plugin ->
                async { plugin.collectAndExportMetrics(workLoad, testUUID) }
            }
        }.awaitAll()
    }
}