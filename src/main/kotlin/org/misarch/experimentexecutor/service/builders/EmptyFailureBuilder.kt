package org.misarch.experimentexecutor.service.builders

import MiSArchFailureConfig
import org.misarch.experimentexecutor.plugin.failure.chaostoolkit.model.ChaosToolkitConfig
import java.util.UUID

fun buildEmptyChaosToolkitConfig(
    testUUID: UUID,
    testVersion: String,
) = ChaosToolkitConfig(
    title = "$testUUID:$testVersion",
    description = "$testUUID:$testVersion",
    steadyStateHypothesis = null,
    method = listOf(),
)

fun buildEmptyMisarchExperimentConfig() = listOf<MiSArchFailureConfig>()
