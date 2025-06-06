package org.misarch.experimentexecutor.plugin.export.report.model

import org.misarch.experimentexecutor.model.Goal

data class Report(
    val testUUID: String,
    val testVersion: String,
    val startTime: String,
    val endTime: String,
    val goals: List<Goal>,
    val errors: List<Error>,
)
