package org.misarch.experimentexecutor.controller.configuration

import org.misarch.experimentexecutor.controller.model.dto.BatchUpdateVariableDto
import org.misarch.experimentexecutor.controller.model.dto.ServiceConfiguration
import org.misarch.experimentexecutor.controller.model.dto.UpdateVariableDto
import org.misarch.experimentexecutor.service.configuration.ConfigurationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController("/configuration")
class ConfigurationController(
    private val configurationService: ConfigurationService
) {
    @GetMapping
    suspend fun getAllServices(): List<ServiceConfiguration> {
        return configurationService.findAllServices()
    }

    @GetMapping("/names")
    suspend fun getAllServiceNames(): List<String> {
        return configurationService.findAllServiceNames()
    }

    @GetMapping("/{serviceName}/defined-variables")
    suspend fun getServiceDefinedVariables(@PathVariable serviceName: String): List<ServiceConfiguration.ConfigurationVariableDefinition> {
        return configurationService.findService(serviceName)?.variableDefinitions ?: emptyList()
    }

    @GetMapping("/{serviceName}/variables")
    suspend fun getServiceVariables(@PathVariable serviceName: String): List<ServiceConfiguration.ConfigurationVariable> {
        return configurationService.findService(serviceName)?.globalVariables ?: emptyList()
    }

    @GetMapping("/{serviceName}/replicas")
    suspend fun getServiceReplicas(@PathVariable serviceName: String): List<ServiceConfiguration.ServiceReplica> {
        return configurationService.findService(serviceName)?.replicas ?: emptyList()
    }

    @PutMapping("/{serviceName}/variables")
    suspend fun updateServiceVariables(@PathVariable serviceName: String, @RequestBody batchUpdateDto: BatchUpdateVariableDto) {
        return configurationService.batchAddOrUpdateServiceVariables(serviceName, batchUpdateDto.variables)
    }

    @GetMapping("/{serviceName}/variables/{variableName}")
    suspend fun getServiceVariable(@PathVariable serviceName: String, @PathVariable variableName: String): ServiceConfiguration.ConfigurationVariable {
        return configurationService.getServiceVariable(serviceName, variableName)
    }

    @PutMapping("/{serviceName}/variables/{variableName}")
    suspend fun updateServiceVariable(
        @PathVariable serviceName: String,
        @PathVariable variableName: String,
        @RequestBody updateDto: UpdateVariableDto) {
        val updatedVariable = ServiceConfiguration.ConfigurationVariable(variableName, updateDto.value)
        return configurationService.batchAddOrUpdateServiceVariables(serviceName, listOf(updatedVariable))
    }

    @GetMapping("/{serviceName}/replicas/{replicaId}/variables")
    suspend fun getReplicaVariables(
        @PathVariable serviceName: String,
        @PathVariable replicaId: String
    ): List<ServiceConfiguration.ConfigurationVariable> {
        return configurationService.findReplica(serviceName, replicaId).replicaVariables
    }

    @PutMapping("/{serviceName}/replicas/{replicaId}/variables")
    suspend fun updateReplicaVariables(
        @PathVariable serviceName: String,
        @PathVariable replicaId: String,
        @RequestBody batchUpdateDto: BatchUpdateVariableDto
    ) {
        return configurationService.batchAddOrUpdateReplicaVariables(serviceName, replicaId, batchUpdateDto.variables)
    }

    @GetMapping("/{serviceName}/replicas/{replicaId}/variables/{variableName}")
    suspend fun getReplicaVariable(
        @PathVariable serviceName: String,
        @PathVariable replicaId: String,
        @PathVariable variableName: String
    ): ServiceConfiguration.ConfigurationVariable {
        return configurationService.getReplicaVariable(serviceName, replicaId, variableName)
    }

    @PutMapping("/{serviceName}/replicas/{replicaId}/variables/{variableName}")
    suspend fun updateReplicaVariable(
        @PathVariable serviceName: String,
        @PathVariable replicaId: String,
        @PathVariable variableName: String,
        @RequestBody updateDto: UpdateVariableDto
    ) {
        val updatedVariable = ServiceConfiguration.ConfigurationVariable(variableName, updateDto.value)
        return configurationService.batchAddOrUpdateReplicaVariables(serviceName, replicaId, listOf(updatedVariable))
    }
}
