package org.misarch.experimentexecutor.model

data class ExperimentConfig(
    val testUUID: String,
    val testVersion: String,
    val testName: String,
    val workLoad: WorkLoad,
    val goals: List<Goal>,
)

data class WorkLoad(
    val gatling: GatlingConfig,
)

data class GatlingConfig(
    val loadType: GatlingLoadType,
)

enum class GatlingLoadType {
    ScalabilityLoadTest,
    ResilienceLoadTest,
    ElasticityLoadTest,
    NormalLoadTest,
}

data class Goal(
    val metric: String,
    val threshold: String,
    val color: String,
)
