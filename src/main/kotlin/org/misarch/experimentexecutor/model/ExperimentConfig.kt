package org.misarch.experimentexecutor.model

data class ExperimentConfig(
    val testUUID: String,
    val failure: Failure,
    val workLoad: WorkLoad,
    val goals: List<Goal>
)

data class Failure(
    val chaosToolkit: ChaosToolKitConfig,
    val experimentConfig: MiSArchExperimentConfig,
)

data class ChaosToolKitConfig(
    val pathUri: String
)

data class MiSArchExperimentConfig(
    val pathUri: String,
    val endpointHost: String,
)

data class WorkLoad(
    val gatling: GatlingConfig,
)

data class GatlingConfig(
    val loadType: GatlingLoadType,
    val userStepsPathUri: String,
    val workPathUri: String,
    val endpointHost: String,
    val tokenEndpointHost: String,
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