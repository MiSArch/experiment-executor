package org.misarch.experimentexecutor.plugin.failure.chaostoolkit.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ChaosToolkitConfig(
    val title: String,
    val description: String,
    @JsonProperty("steady-state-hypothesis") val steadyStateHypothesis: SteadyStateHypothesis? = null,
    val method: List<Method>,
    val rollbacks: List<Action>? = null,
    val tags: List<String>? = null,
    val configuration: Map<String, Any>? = null,
    val secrets: Map<String, Map<String, Any>>? = null,
    val extensions: List<Map<String, Any>>? = null,
    val contributions: Map<String, String>? = null,
    val controls: List<Control>? = null,
    val runtime: Map<String, Any>? = null,
)

sealed interface Method

data class Probe(
    val type: String = "probe",
    val name: String,
    val provider: Provider,
    val tolerance: Any? = null,
    val configuration: List<String>? = null,
    val background: Boolean? = null,
    val controls: List<Control>? = null,
) : Method

data class Action(
    val type: String = "action",
    val name: String,
    val provider: Provider,
    val pauses: Pause? = null,
    val configuration: List<String>? = null,
    val background: Boolean? = null,
    val controls: List<Control>? = null,
) : Method

data class SteadyStateHypothesis(
    val title: String,
    val probes: List<Probe>,
    val controls: List<Control>? = null,
)

sealed interface Provider

data class PythonProvider(
    val type: String = "python",
    val module: String,
    val func: String,
    val arguments: Map<String, Any>? = null,
    val secrets: Map<String, Map<String, Any>>? = null,
) : Provider

data class HttpProvider(
    val type: String = "http",
    val url: String,
    val method: String? = null,
    val headers: Map<String, String>? = null,
    @JsonProperty("expected-status") val expectedStatus: Int? = null,
    val arguments: Map<String, Any>? = null,
    val timeout: Int? = null,
    val secrets: Map<String, Map<String, Any>>? = null,
) : Provider

data class ProcessProvider(
    val type: String = "process",
    val path: String,
    val arguments: Any? = null, // String or List<String>
    val timeout: Int? = null,
    val secrets: Map<String, Map<String, Any>>? = null,
) : Provider

data class Pause(
    val before: Int,
    val after: Int,
)

data class Control(
    val name: String,
    val provider: PythonProvider,
    val scope: String? = null, // "before" or "after"
    val automatic: Boolean? = null,
)
