package org.misarch.experimentexecutor.model

data class ExperimentConfig(
    val testUUID: String,
    val testVersion: String,
    val testName: String,
    val loadType: GatlingLoadType,
    val goals: List<Goal>,
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
