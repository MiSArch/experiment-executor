package org.misarch.experimentexecutor.plugin.export.report.model

import org.misarch.experimentexecutor.model.Goal

data class Report(
    val testUUID: String,
    val testVersion: String,
    val experimentExecutorVersion: String,
    val startTime: String,
    val endTime: String,
    val goals: List<Goal>,
    val goalViolations: List<GoalViolation>,
)

data class GoalViolation(
    val metricName: String,
    val threshold: String,
    val actualValue: String,
)
