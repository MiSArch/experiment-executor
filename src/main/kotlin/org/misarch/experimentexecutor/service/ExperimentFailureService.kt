package org.misarch.experimentexecutor.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.ChaosToolkitPlugin
import org.misarch.experimentexecutor.plugin.failure.misarch.MisarchExperimentConfigPlugin
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

@Service
class ExperimentFailureService(
    webClient: WebClient,
    @Value("\${misarch.experiment-config.host}") private val misarchExperimentConfigHost: String,
    @Value("\${experiment-executor.url}") private val experimentExecutorUrl: String,
    @Value("\${chaostoolkit.executor-host}") private val chaosToolkitExecutorHost: String,
    @Value("\${experiment-executor.base-path}") private val basePath: String,
    @Value("\${misarch.experiment-config.active}") private val misarchExperimentConfigActive: Boolean,
) {
    val registry =
        if (misarchExperimentConfigActive) {
            listOf(
                ChaosToolkitPlugin(webClient, chaosToolkitExecutorHost, basePath),
                MisarchExperimentConfigPlugin(webClient, misarchExperimentConfigHost, experimentExecutorUrl, basePath),
            )
        } else {
            listOf(ChaosToolkitPlugin(webClient, chaosToolkitExecutorHost, basePath))
        }

    suspend fun setupExperimentFailure(
        testUUID: UUID,
        testVersion: String,
    ) {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.initializeFailure(testUUID, testVersion) }
            }
        }.awaitAll()
    }

    suspend fun startExperimentFailure(
        testUUID: UUID,
        testVersion: String,
    ) {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.startTimedExperiment(testUUID, testVersion) }
            }
        }.awaitAll()
    }

    suspend fun stopExperimentFailure(
        testUUID: UUID,
        testVersion: String,
    ) {
        coroutineScope {
            registry.map { plugin ->
                async { plugin.stopExperiment(testUUID, testVersion) }
            }
        }.awaitAll()
    }
}
