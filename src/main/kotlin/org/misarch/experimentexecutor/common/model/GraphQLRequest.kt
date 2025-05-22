package org.misarch.experimentexecutor.common.model

abstract class GraphQLRequest(
    open val inputs: Map<String, String>?,
)

data class Mutation(
    override val inputs: Map<String, String>?,
) : GraphQLRequest(inputs)

data class Query(
    override val inputs: Map<String, String>?,
) : GraphQLRequest(inputs)