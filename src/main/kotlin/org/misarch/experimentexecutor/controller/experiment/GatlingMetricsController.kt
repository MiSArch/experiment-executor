package org.misarch.experimentexecutor.controller.experiment

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import java.io.File
import java.util.*

@RestController
class GatlingMetricsController(
    @Value("\${experiment-executor.base-path:}") val basePath: String,
) {

    /**
     * Collects Gatling metrics from Gatling's index.html and saves them to a file.
     * The HTML content is expected to be sent in the request body.
     */
    @PostMapping("/experiment/{testUUID}/gatling/metrics/html")
    private suspend fun collectGatingMetricsHtml(@PathVariable testUUID: UUID, @RequestBody html: String) {
        writeFile(testUUID, fileName = "gatling-index.html", content = html)

    }

    /**
     * Collects Gatling metrics stats from Gatling's stats.js and saves them to a file.
     * The stats content is expected to be sent in the request body.
     */
    @PostMapping("/experiment/{testUUID}/gatling/metrics/stats")
    private suspend fun collectGatingMetricsStats(@PathVariable testUUID: UUID, @RequestBody stats: String) {
      writeFile(testUUID, fileName = "gatling-stats.js", content = stats)
    }

    private suspend fun writeFile(testUUID: UUID, fileName: String, content: String) {
        val experimentDir = "$basePath/$testUUID"
        val filePath = "$experimentDir/$fileName"
        File(filePath).writeText(content)
    }
}