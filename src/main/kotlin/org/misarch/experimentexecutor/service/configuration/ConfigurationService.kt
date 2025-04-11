package org.misarch.experimentexecutor.service.configuration

import org.misarch.experimentexecutor.controller.model.dto.ServiceConfiguration
import org.springframework.stereotype.Service

@Service
class ConfigurationService {

    // TODO Repo and inner stuff

    suspend fun heartbeat(serviceName: String, replicaId: String) {
        // TODO implement
    }

    private suspend fun handleFirstHeartbeat(serviceName: String, replicaId: String) {
        // TODO implement
    }

    private suspend fun addService(serviceName: String, initialReplicaId: String) {
        // TODO implement
    }

    // TODO signature needs a third parameter configuration
    private suspend fun buildServiceConfiguration(serviceName: String, replicaId: String): ServiceConfiguration {
        // TODO implement
        return ServiceConfiguration(
            name = serviceName,
            replicas = emptyList(),
            globalVariables = emptyList(),
            variableDefinitions = emptyList(),
        )
    }

    private suspend fun addReplica(serviceName: String, replicaId: String): ServiceConfiguration {
        // TODO implement
        return ServiceConfiguration(
            name = serviceName,
            replicas = emptyList(),
            globalVariables = emptyList(),
            variableDefinitions = emptyList(),
        )
    }

    suspend fun findAllServices(): List<ServiceConfiguration> {
        // TODO implement
        return emptyList()
    }

    suspend fun findAllServiceNames(): List<String> {
        return findAllServices().map { it.name }
    }

    suspend fun findService(serviceName: String): ServiceConfiguration? {
        // TODO implement
        return ServiceConfiguration(
            name = serviceName,
            replicas = emptyList(),
            globalVariables = emptyList(),
            variableDefinitions = emptyList(),
        )
    }

    suspend fun getServiceVariable(
        serviceName: String,
        variableName: String
    ): ServiceConfiguration.ConfigurationVariable {
        return findService(serviceName)?.globalVariables?.find { it.name == variableName }
            ?: throw IllegalArgumentException("Variable $variableName not found")
    }

    suspend fun findReplica(serviceName: String, replicaId: String): ServiceConfiguration.ServiceReplica {
        val service = findService(serviceName) ?: throw IllegalArgumentException("Service $serviceName not found")
        return service.replicas.find { it.id == replicaId }
            ?: throw IllegalArgumentException("Replica $replicaId not found")
    }

    suspend fun getReplicaVariable(
        serviceName: String,
        replicaId: String,
        variableName: String
    ): ServiceConfiguration.ConfigurationVariable {
        return findReplica(serviceName, replicaId)?.replicaVariables?.find { it.name == variableName }
            ?: throw IllegalArgumentException("Variable $variableName not found")
    }

    suspend fun batchAddOrUpdateServiceVariables(
        serviceName: String,
        variables: List<ServiceConfiguration.ConfigurationVariable>
    ) {
        // TODO implement
    }

    private suspend fun updateServiceVariables(
        service: ServiceConfiguration,
        variables: List<ServiceConfiguration.ConfigurationVariable>
    ) {
        service.globalVariables.forEach { variable ->
            val updated = variables.find { it.name == variable.name }
            if (updated != null) {
                // TODO implement
            }
        }
    }

    suspend fun batchAddOrUpdateReplicaVariables(
        serviceName: String,
        replicaId: String,
        variables: List<ServiceConfiguration.ConfigurationVariable>
    ) {
        // TODO implement
    }

    private suspend fun updateReplicaVariables(
        replicaVariables: List<ServiceConfiguration.ConfigurationVariable>,
        variables: List<ServiceConfiguration.ConfigurationVariable>
    ) {
        // TODO implement
    }

    private suspend fun validateVariables(variables: List<ServiceConfiguration.ConfigurationVariable>, serviceName: String) {
        // TODO implement
    }

    private suspend fun deleteService(name: String): Boolean {
        // TODO implement
        return false
    }

    private suspend fun deleteReplica(name: String, replicaId: String): Boolean {
        // TODO implement
        return false
    }
}