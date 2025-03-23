package org.misarch.experimentconfignew.executor.model

import java.net.URI

data class ExperimentConfig(
    val failure: Failure,
    val load: Load,
    val metrics: List<Metric>
)

data class Failure(
    val infrastructure: Infrastructure,
    val container: Container,
    // Here could be more necessary
)

data class Infrastructure(
    val kubernetes: Kubernetes
)

data class Kubernetes(
    val nodes: List<Node>,
    val pods: List<Pod>
)

data class Node(
    val shutdown: Boolean
)

data class Pod(
    val shutdown: Boolean
)

data class Container(
    val availability: List<String>,
    val latency: List<Latency>,
    val response: List<Response>,
    val resources: Resources
)

data class Latency(
    val path: String,
    val latency: Int
)

data class Response(
    val path: String,
    val status: Int
)

data class Resources(
    val cpu: Int,
    val memory: Int
)

data class Load(
    val graphql: GraphQL?,
    //val rest: Map<Any, Any>
)

data class GraphQL(
    val uri: URI,
    val flows: List<Flow>
)

data class Flow(
    val name: String,
    val description: String?,
    val maxDurationPerIteration: Int,
    val maxIterationsPerVirtualUser: Int,
    val parallelVirtualUsers: Int,
    val dynamicLoadProfile: DynamicLoadProfile?,
    val requests: List<Request>
)

data class DynamicLoadProfile(
    val increasing: Int,
    val decreasing: Int,
    val duration: Int
)

data class Request(
    val name: String,
    val inputs: List<Input>?,
    val outputs: List<Output>?,
)

data class Input (
    val refInput: Output?,
    val value: Map<String, Any>?,
)

data class Output (
    val name: String,
    val list: List<Pair<String, Any>>?,
    val value: String?,
)

data class Metric(
    val name: String,
    val description: String,
    val unit: String,
    val threshold: List<String>
)