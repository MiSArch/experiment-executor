package org.misarch.experimentexecutor.plugin.export.grafana

import org.misarch.experimentexecutor.plugin.export.ExportPluginInterface
import java.util.*

class GrafanaPlugin : ExportPluginInterface {
    override suspend fun createReport(testUUID: UUID): Boolean {
        return true
    }
}