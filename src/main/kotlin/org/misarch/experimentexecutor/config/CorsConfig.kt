package org.misarch.experimentexecutor.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer


@Configuration
@EnableWebFlux
class CorsGlobalConfiguration(
    private val experimentExecutorConfig: ExperimentExecutorConfig
) : WebFluxConfigurer {
    override fun addCorsMappings(corsRegistry: CorsRegistry) {
        corsRegistry.addMapping("/**")
            .allowedOrigins(*experimentExecutorConfig.corsOrigins.toTypedArray())
            .allowedMethods("PUT", "POST", "GET", "DELETE", "OPTIONS")
            .maxAge(3600)
    }
}