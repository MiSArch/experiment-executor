package org.misarch.experimentexecutor.plugin.export.grafana

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import org.jsoup.Jsoup
import org.misarch.experimentexecutor.config.GRAFANA_DASHBOARD_FILENAME
import org.misarch.experimentexecutor.config.GRAFANA_DASHBOARD_FILENAME_KUBERNETES
import org.misarch.experimentexecutor.config.GrafanaConfig
import org.misarch.experimentexecutor.config.REPORT_FILENAME
import org.misarch.experimentexecutor.config.TEMPLATE_PREFIX
import org.misarch.experimentexecutor.model.Goal
import org.misarch.experimentexecutor.plugin.export.ExportPluginInterface
import org.misarch.experimentexecutor.plugin.export.grafana.model.GrafanaDashboardConfig
import org.misarch.experimentexecutor.plugin.export.grafana.model.Option
import org.misarch.experimentexecutor.plugin.export.report.model.Report
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import reactor.core.publisher.Mono
import java.io.File
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

class GrafanaPlugin(
    private val webClient: WebClient,
    private val grafanaConfig: GrafanaConfig,
    private val templatePath: String,
    private val basePath: String,
    private val isKubernetes: Boolean,
) : ExportPluginInterface {
    private var serviceAccountId: String? = null
    private var apiToken: String? = null

    override suspend fun createReport(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        goals: List<Goal>,
        gatlingStatsHtml: String,
    ) {
        val filePath =
            if (isKubernetes) {
                "$templatePath/${TEMPLATE_PREFIX}${GRAFANA_DASHBOARD_FILENAME_KUBERNETES}"
            } else {
                "$templatePath/${TEMPLATE_PREFIX}${GRAFANA_DASHBOARD_FILENAME}"
            }

        createResultDashboard(filePath, testUUID, testVersion, startTime, endTime, goals, gatlingStatsHtml)
    }

    private suspend fun createResultDashboard(
        filePath: String,
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
        endTime: Instant,
        goals: List<Goal>,
        gatlingStatsHtml: String,
    ) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $filePath")
        }
        val dashboardTemplate = file.readText()
        val gatlingErrors = extractErrorsTableHtml(gatlingStatsHtml)
        val pastExecutionDiffs = getPastExecutionDiffs(testUUID, testVersion, startTime)

        val updatedContent =
            dashboardTemplate
                .replace("REPLACE_ME_TEST_UUID", testUUID.toString())
                .replace("REPLACE_ME_TEST_VERSION", testVersion)
                .replace("REPLACE_ME_TEST_START_TIME", startTime.toString())
                .replace("REPLACE_ME_TEST_END_TIME", endTime.toString())
                .replace("REPLACE_ME_TIME_SHIFT", "${pastExecutionDiffs.minOrNull() ?: 0}s")
                .replace("REPLACE_ME_GATLING_ERRORS", gatlingErrors)

        val templateParsed = jacksonObjectMapper().readValue(updatedContent, GrafanaDashboardConfig::class.java)
        val finalDashboard = templateParsed.applyDynamicChangesToDashboard(goals, pastExecutionDiffs)

        deleteAndCreateDashboard(testUUID, testVersion, finalDashboard)
        logger.info { "\uD83D\uDCC8 Result dashboard exported to Grafana\n http://localhost:3001/d/$testUUID-$testVersion" }
    }

    private suspend fun fetchServiceAccountByName(serviceAccountName: String): String? {
        val response =
            webClient
                .get()
                .uri("${grafanaConfig.url}/api/serviceaccounts/search?perpage=10&page=1&query=$serviceAccountName")
                .headers { it.setBasicAuth(grafanaConfig.adminUser, grafanaConfig.adminPassword) }
                .retrieve()
                .bodyToMono(Map::class.java)
                .awaitSingle()

        val serviceAccount =
            (response["serviceAccounts"] as List<*>)
                .filterIsInstance<Map<*, *>>()
                .find { it["name"] == serviceAccountName }

        return serviceAccount?.get("id")?.toString()
    }

    private suspend fun createServiceAccount(
        serviceAccountName: String,
        serviceAccountRole: String,
    ): String {
        val existingServiceAccountId = fetchServiceAccountByName(serviceAccountName)
        if (existingServiceAccountId != null) {
            serviceAccountId = existingServiceAccountId
            return serviceAccountId!!
        }

        val payload = mapOf("name" to serviceAccountName, "role" to serviceAccountRole)

        val response =
            webClient
                .post()
                .uri("${grafanaConfig.url}/api/serviceaccounts")
                .contentType(MediaType.APPLICATION_JSON)
                .headers { it.setBasicAuth(grafanaConfig.adminUser, grafanaConfig.adminPassword) }
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map::class.java)
                .awaitSingle()

        serviceAccountId = response["id"]?.toString()
            ?: throw IllegalStateException("Failed to parse service account ID.")
        return serviceAccountId!!
    }

    private suspend fun deleteAndCreateDashboard(
        testUUID: UUID,
        testVersion: String,
        finalDashboard: GrafanaDashboardConfig,
    ) {
        val grafanaToken = createApiToken("experiment-executor-${UUID.randomUUID()}")
        webClient
            .post()
            .uri("${grafanaConfig.url}/api/dashboards/db")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $grafanaToken")
            .bodyValue(jacksonObjectMapper().writeValueAsString(finalDashboard))
            .retrieve()
            .onStatus({ it.value() != 200 && it.value() != 201 && it.value() != 204 }) { response ->
                Mono.error(IllegalStateException("Dashboard creation failed for testUUID: $testUUID and testVersion: $testVersion"))
            }.awaitBodilessEntity()
    }

    private suspend fun createApiToken(tokenName: String): String {
        if (apiToken != null) {
            return apiToken!!
        }
        if (serviceAccountId == null) {
            createServiceAccount("experiment-executor", "Admin")
        }

        val payload = mapOf("name" to tokenName)

        val response =
            webClient
                .post()
                .uri("${grafanaConfig.url}/api/serviceaccounts/$serviceAccountId/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .headers { it.setBasicAuth(grafanaConfig.adminUser, grafanaConfig.adminPassword) }
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map::class.java)
                .awaitSingle()

        apiToken = response["key"]?.toString()
            ?: throw IllegalStateException("Failed to parse API token.")
        return apiToken!!
    }

    private fun getPastExecutionDiffs(
        testUUID: UUID,
        testVersion: String,
        startTime: Instant,
    ): List<Int> {
        val reportDir = File("$basePath/$testUUID/$testVersion/reports")
        val reportDirs =
            reportDir
                .listFiles()
                ?.filter { it.isDirectory }
                ?.map { it.name }
                ?: emptyList()

        val timeStamps =
            reportDirs.mapNotNull { dir ->
                val reportFile = File("$reportDir/$dir/$REPORT_FILENAME")
                if (reportFile.exists()) {
                    runCatching {
                        val reportContent = reportFile.readText()
                        val reportData = jacksonObjectMapper().readValue(reportContent, Report::class.java)
                        reportData.startTime
                    }.getOrNull()
                } else {
                    null
                }
            }

        return timeStamps
            .map { timeStamp ->
                ((startTime.toEpochMilli() - timeStamp.toLong()) / 1000).toInt()
            }.filter { it > 0 }
            .sorted()
    }

    private fun extractErrorsTableHtml(html: String): String {
        val document = Jsoup.parse(html)
        val table = document.getElementById("container_errors")
        return table
            ?.outerHtml()
            ?.replace(Regex(">\\s+<"), "><")
            ?.trim()
            ?.replace("\"", "\\\"") ?: ""
    }

    private fun GrafanaDashboardConfig.applyDynamicChangesToDashboard(
        goals: List<Goal>,
        pastExecutionDiffs: List<Int>,
    ) = copy(
        dashboard =
            this.dashboard.copy(
                panels =
                    this.dashboard.panels?.map {
                        it.copy(
                            fieldConfig =
                                it.fieldConfig?.copy(
                                    defaults =
                                        it.fieldConfig.defaults?.copy(
                                            thresholds =
                                                if (goals.any { goal -> goal.metric == it.title }) {
                                                    it.fieldConfig.defaults.thresholds?.copy(
                                                        mode = it.fieldConfig.defaults.thresholds.mode,
                                                        steps =
                                                            it.fieldConfig.defaults.thresholds.steps.flatMap { step ->
                                                                val goal = goals.first { goal -> goal.metric == it.title }
                                                                listOf(
                                                                    step.copy(),
                                                                    step.copy(
                                                                        color = goal.color,
                                                                        value = goal.threshold.toDouble(),
                                                                    ),
                                                                )
                                                            },
                                                    )
                                                } else {
                                                    it.fieldConfig.defaults.thresholds
                                                },
                                        ),
                                ),
                        )
                    },
                templating =
                    this.dashboard.templating?.copy(
                        list =
                            this.dashboard.templating.list.map { variable ->
                                if (variable.name != "timeShift") {
                                    variable
                                } else {
                                    variable.copy(
                                        options =
                                            pastExecutionDiffs.map { diff ->
                                                Option(
                                                    text = "${diff}s",
                                                    value = "${diff}s",
                                                    selected = diff == pastExecutionDiffs.minOrNull(),
                                                )
                                            },
                                        query =
                                            if (pastExecutionDiffs.isNotEmpty()) {
                                                pastExecutionDiffs.joinToString(
                                                    ",",
                                                ) { "${it}s" }
                                            } else {
                                                "0s"
                                            },
                                    )
                                }
                            },
                    ),
            ),
    )
}
