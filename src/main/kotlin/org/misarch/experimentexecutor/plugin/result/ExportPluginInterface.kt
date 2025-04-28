package org.misarch.experimentexecutor.plugin.result

import java.time.Instant
import java.util.*

interface ExportPluginInterface {
    suspend fun createReport(testUUID: UUID, startTime: Instant, endTime: Instant): Boolean
}