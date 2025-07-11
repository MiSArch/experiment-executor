package org.misarch.experimentexecutor.model

data class ExperimentConfig(
    val testUUID: String,
    val testVersion: String,
    val testName: String,
    val loadType: GatlingLoadType,
    val goals: List<Goal>,
    val warmUp: WarmUp? = null,
    val steadyState: SteadyState? = null,
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

data class WarmUp(
    val duration: Int,
    val rate: Int,
)

data class SteadyState(
    val duration: Int,
    val rate: Int,
    val factor: Float,
)
