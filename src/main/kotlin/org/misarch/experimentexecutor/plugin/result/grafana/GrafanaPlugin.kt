package org.misarch.experimentexecutor.plugin.result.grafana

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.misarch.experimentexecutor.config.GRAFANA_DASHBOARD_FILENAME
import org.misarch.experimentexecutor.config.GRAFANA_URL
import org.misarch.experimentexecutor.config.TEMPLATE_PATH
import org.misarch.experimentexecutor.config.TEMPLATE_PREFIX
import org.misarch.experimentexecutor.model.Goal
import org.misarch.experimentexecutor.plugin.result.ExportPluginInterface
import org.misarch.experimentexecutor.plugin.result.grafana.model.GrafanaDashboardConfig
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import java.io.File
import java.time.Instant
import java.util.*

class GrafanaPlugin(private val webClient: WebClient, private val grafanaApiToken: String) : ExportPluginInterface {

    override suspend fun createReport(testUUID: UUID, startTime: Instant, endTime: Instant, goals: List<Goal>): Boolean {
        val filePath = "$TEMPLATE_PATH/${TEMPLATE_PREFIX}${GRAFANA_DASHBOARD_FILENAME}"
        return updateDashboardTemplate(filePath, testUUID, startTime, endTime, goals)
    }

    private suspend fun updateDashboardTemplate(
        filePath: String,
        testUUID: UUID,
        startTime: Instant,
        endTime: Instant,
        goals: List<Goal>,
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

        val dashboardParsed = jacksonObjectMapper().readValue(updatedContent, GrafanaDashboardConfig::class.java)
        val updatedDashboard = dashboardParsed.copy(
            dashboard = dashboardParsed.dashboard.copy(
                panels = dashboardParsed.dashboard.panels?.map {
                    it.copy(
                        fieldConfig = it.fieldConfig?.copy(
                            defaults = it.fieldConfig.defaults?.copy(
                                thresholds =
                                    if (goals.any { goal -> goal.metric == it.title }) {
                                        it.fieldConfig.defaults.thresholds?.copy(
                                            mode = it.fieldConfig.defaults.thresholds.mode,
                                            steps = it.fieldConfig.defaults.thresholds.steps.flatMap { step ->
                                                val goal = goals.first { goal -> goal.metric == it.title }
                                                listOf(step.copy(),
                                                step.copy(
                                                    color = goal.color,
                                                    value = goal.threshold.toDouble()
                                                ))
                                            })
                                    } else it.fieldConfig.defaults.thresholds
                            )
                        )
                    )
                }
            )
        )

        webClient.post()
            .uri("$GRAFANA_URL/api/dashboards/db")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $grafanaApiToken")
            .bodyValue(jacksonObjectMapper().writeValueAsString(updatedDashboard))
            .retrieve()
            .awaitBodilessEntity()

        println("\uD83D\uDCC8 Result dashboard exported to Grafana\n $GRAFANA_URL/d/$testUUID")

        return true
    }
}