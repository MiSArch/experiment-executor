package org.misarch.experimentexecutor.plugin.result.grafana

import org.misarch.experimentexecutor.plugin.result.ExportPluginInterface
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import java.io.File
import java.time.Instant
import java.util.*

class GrafanaPlugin(private val webClient: WebClient, private val grafanaApiToken: String) : ExportPluginInterface {

    override suspend fun createReport(testUUID: UUID, startTime: Instant, endTime: Instant): Boolean {
        // TODO
        val filePath = "src/main/resources/dashboards/experiment-dashboard-template.json"
        return updateDashboardTemplate(filePath, testUUID, startTime, endTime)
    }

    private suspend fun updateDashboardTemplate(
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


        webClient.post()
            .uri("http://localhost:3001/api/dashboards/db")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $grafanaApiToken")
            .bodyValue(updatedContent)
            .retrieve()
            .awaitBodilessEntity()

        println("\uD83D\uDCC8 Result dashboard exported to Grafana\n http://localhost:3001/d/$testUUID")

        return true
    }
}