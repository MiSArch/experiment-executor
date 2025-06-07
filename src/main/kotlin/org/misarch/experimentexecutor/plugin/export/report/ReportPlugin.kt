package org.misarch.experimentexecutor.plugin.export.report

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.misarch.experimentexecutor.config.REPORT_FILENAME
import org.misarch.experimentexecutor.model.Goal
import org.misarch.experimentexecutor.plugin.export.ExportPluginInterface
import org.misarch.experimentexecutor.plugin.export.report.model.Report
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class ReportPlugin(
    private val basePath: String,
) : ExportPluginInterface {
    override suspend fun createReport(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        goals: List<Goal>,
    ) {
        val report =
            Report(
                testUUID = testUUID.toString(),
                testVersion = testVersion,
                startTime = startTime.toEpochMilli().toString(),
                endTime = endTime.toEpochMilli().toString(),
                // TODO somewhere reflect errors when they happen, probably in the error handler
                errors = emptyList(),
                goals = goals,
            )

        val folderName = startTime.truncatedTo(ChronoUnit.SECONDS).toString().replace(":", "-")
        val filePath = "$basePath/$testUUID/$testVersion/reports/$folderName"
        File(filePath).mkdirs()
        File("$filePath/$REPORT_FILENAME").writeText(jacksonObjectMapper().writeValueAsString(report))
    }
}
