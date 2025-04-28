package org.misarch.experimentexecutor.plugin.result.grafana

import org.misarch.experimentexecutor.plugin.result.ExportPluginInterface
import java.io.File
import java.nio.file.Paths
import java.time.Instant
import java.util.*

class GrafanaPlugin : ExportPluginInterface {
    override suspend fun createReport(testUUID: UUID, startTime: Instant, endTime: Instant): Boolean {
        // TODO
        val filePath = "/Users/p371728/master/thesis/misarch/experiment-executor/dashboards/experiment-dashboard-template.json"
        return updateDashboardTemplate(filePath, testUUID, startTime, endTime)
    }

    private fun updateDashboardTemplate(
        filePath: String,
        testUUID: UUID,
        startTime: Instant,
        endTime: Instant
    ): Boolean {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $filePath")
        }
        val content = file.readText()

        val updatedContent = content
            .replace("REPLACE_ME_TEST_UUID", testUUID.toString())
            .replace("REPLACE_ME_TEST_START_TIME", startTime.toString())
            .replace("REPLACE_ME_TEST_END_TIME", endTime.toString())

        val outputPath = Paths.get(file.parent, "experiment-dashboard-$testUUID.json").toString()
        File(outputPath).writeText(updatedContent)

        return true
    }
}