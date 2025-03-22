package org.misarch.experimentconfignew.controller.model.dto

import java.time.Instant

data class ServiceConfiguration(
    val name: String,
    val replicas: List<ServiceReplica>,
    val globalVariables: List<ConfigurationVariable>,
    val variableDefinitions: List<ConfigurationVariableDefinition>,
) {
    data class ServiceReplica(
        val id: String,
        val replicaVariables: List<ConfigurationVariable>,
        // TODO check this is correctly formatted
        val lastSeen: Instant?,
    )

    data class ConfigurationVariable(
        val name: String,
        val value: Any,
    )

    data class ConfigurationVariableDefinition(
        val key: String,
        // TODO this should be a JsonSchemaType
        val type: String,
        val defaultValue: Any,
    )
}



