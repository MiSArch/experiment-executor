package org.misarch.experimentexecutor.model

data class ExperimentConfig(
    val testUUID: String,
    val workLoad: WorkLoad,
    val goals: List<Goal>
)

data class WorkLoad(
    val gatling: GatlingConfig,
)

data class GatlingConfig(
    val loadType: GatlingLoadType,
    val endpointHost: String,
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