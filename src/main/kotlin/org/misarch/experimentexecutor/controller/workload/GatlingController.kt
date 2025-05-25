package org.misarch.experimentexecutor.controller.workload

import org.springframework.web.bind.annotation.*
import java.io.File
import java.util.*

@RestController
@CrossOrigin(origins = ["http://localhost:5173"])
class GatlingController {

    @PostMapping("/experiment/{testUUID}/gatling/metrics/html")
    private suspend fun collectGatingMetricsHtml(@PathVariable testUUID: UUID, @RequestBody html: String) {
        writeFile(testUUID, fileName = "gatling-index.html", content = html)

    }

    @PostMapping("/experiment/{testUUID}/gatling/metrics/stats")
    private suspend fun collectGatingMetricsStats(@PathVariable testUUID: UUID, @RequestBody stats: String) {
      writeFile(testUUID, fileName = "gatling-stats.js", content = stats)
    }

    private suspend fun writeFile(testUUID: UUID, fileName: String, content: String) {
        val basePath = System.getenv("BASE_PATH")
        val experimentDir = "$basePath/$testUUID"
        val filePath = "$experimentDir/$fileName"
        File(filePath).writeText(content)
    }
}