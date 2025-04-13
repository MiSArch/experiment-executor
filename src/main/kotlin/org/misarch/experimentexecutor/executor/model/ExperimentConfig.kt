package org.misarch.experimentexecutor.executor.model

data class ExperimentConfig(
    val failure: Failure,
    val workLoad: WorkLoad,
    val metrics: List<Metric>,
    val goals: Goals
)

data class Failure(
    val chaosToolkit: ChaosToolKitConfig?,
    val experimentConfig: MiSArchExperimentConfig?,
)

data class ChaosToolKitConfig(
    val pathUri: String
)

data class MiSArchExperimentConfig(
    val pathUri: String
)

data class WorkLoad(
   val gatling: GatlingConfig?,
)

data class GatlingConfig(
    val pathUri: String
)

data class Metric(
    val name: String,
    val description: String,
    val unit: String,
)

data class Goals(
    val user: List<Goal>,
    val system: List<Goal>
)
data class Goal(
    val metric: String,
    val threshold: String
)