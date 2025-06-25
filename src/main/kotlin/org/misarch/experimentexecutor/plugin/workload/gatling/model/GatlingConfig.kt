package org.misarch.experimentexecutor.plugin.workload.gatling.model

data class GatlingConfig(
    val fileName: String,
    val workFileContent: String,
    val userStepsFileContent: String,
)
