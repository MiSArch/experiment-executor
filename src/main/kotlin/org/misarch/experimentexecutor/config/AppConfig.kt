package org.misarch.experimentexecutor.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationPropertiesScan
@EnableConfigurationProperties(GrafanaConfig::class, ExperimentExecutorConfig::class)
class AppConfig

@ConfigurationProperties(prefix = "grafana")
data class GrafanaConfig(
    val adminUser: String,
    val adminPassword: String,
    val url: String,
)

@ConfigurationProperties(prefix = "experiment-executor")
data class ExperimentExecutorConfig(
    val templatePath: String,
    val basePath: String,
    val storeResultDataInFiles: Boolean,
    val corsOrigins: List<String>,
)
