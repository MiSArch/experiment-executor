package org.misarch.experimentexecutor.plugin.export.llm

import org.misarch.experimentexecutor.model.Goal
import org.misarch.experimentexecutor.plugin.export.ExportPluginInterface
import java.time.Instant
import java.util.*

class LLMPlugin : ExportPluginInterface {
    override suspend fun createReport(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        goals: List<Goal>,
    ) {
        // Here you can wait until the Gatling and Prometheus plugin wrote their data to files, read in the files and forward all that data
        // to a LLM to see if it can make any sense out of it. It might be helpful also to add the dashboard template or at least its queries.
    }
}
