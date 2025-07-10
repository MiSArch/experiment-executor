package org.misarch.experimentexecutor.plugin.export.report

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.misarch.experimentexecutor.config.REPORT_FILENAME
import org.misarch.experimentexecutor.model.Goal
import org.misarch.experimentexecutor.plugin.export.ExportPluginInterface
import org.misarch.experimentexecutor.plugin.export.report.model.Report
import org.springframework.boot.info.BuildProperties
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class ReportPlugin(
    private val basePath: String,
    private val buildProperties: BuildProperties,
) : ExportPluginInterface {
    override suspend fun createReport(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        goals: List<Goal>,
        gatlingStatsHtml: String,
    ) {
        val folderName = startTime.truncatedTo(ChronoUnit.SECONDS).toString().replace(":", "-")
        val filePath = "$basePath/$testUUID/$testVersion/reports/$folderName"
        File(filePath).mkdirs()

        val report =
            Report(
                testUUID = testUUID.toString(),
                testVersion = testVersion,
                experimentExecutorVersion = buildProperties.version,
                startTime = startTime.toEpochMilli().toString(),
                endTime = endTime.toEpochMilli().toString(),
                goals = goals,
                goalViolations = checkGoalViolations(goals, filePath),
            )

        File("$filePath/$REPORT_FILENAME").writeText(jacksonObjectMapper().writeValueAsString(report))
    }
}
