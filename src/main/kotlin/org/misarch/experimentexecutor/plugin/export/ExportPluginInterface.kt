package org.misarch.experimentexecutor.plugin.export

import java.util.*

interface ExportPluginInterface {
    suspend fun createReport(testUUID: UUID): Boolean
}