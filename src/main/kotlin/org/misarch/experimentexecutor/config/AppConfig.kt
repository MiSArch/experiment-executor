package org.misarch.experimentexecutor.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationPropertiesScan
@EnableConfigurationProperties(GatlingConfig::class, GrafanaConfig::class, ExperimentExecutorConfig::class)
class AppConfig

@ConfigurationProperties(prefix = "gatling")
data class GatlingConfig(
    val targetEndpoint: String,
    val token: TokenConfig
)

@ConfigurationProperties(prefix = "gatling.token")
data class TokenConfig(
    val host: String,
    val clientId: String,
    val path: String,
    val username: String,
    val password: String
)

@ConfigurationProperties(prefix = "grafana")
data class GrafanaConfig(
    val apiToken: String,
    val url: String
)

@ConfigurationProperties(prefix = "experiment-executor")
data class ExperimentExecutorConfig(
    val templatePath: String,
    val basePath: String,
    val url: String,
    val corsOrigins: List<String>,
    val triggerDelay: Long,
)