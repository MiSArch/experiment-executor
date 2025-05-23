package org.misarch.experimentexecutor.plugin.result

import org.misarch.experimentexecutor.executor.model.Goal
import java.time.Instant
import java.util.*

interface ExportPluginInterface {
    suspend fun createReport(testUUID: UUID, startTime: Instant, endTime: Instant, goals: List<Goal>): Boolean
}