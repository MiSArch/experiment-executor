package org.misarch.experimentexecutor.service.builders

import org.misarch.experimentexecutor.model.ExperimentConfig
import org.misarch.experimentexecutor.model.GatlingLoadType
import org.misarch.experimentexecutor.model.Goal
import java.util.UUID

fun buildExperimentConfig(
    testUUID: UUID,
    testVersion: String,
    testName: String,
    loadType: GatlingLoadType,
) = ExperimentConfig(
    testUUID = testUUID.toString(),
    testVersion = testVersion,
    testName = testName,
    loadType = loadType,
    goals =
        listOf(
            Goal(
                metric = "max response time",
                threshold = "2000",
                color = "red",
            ),
            Goal(
                metric = "mean response time",
                threshold = "1000",
                color = "yellow",
            ),
        ),
)
