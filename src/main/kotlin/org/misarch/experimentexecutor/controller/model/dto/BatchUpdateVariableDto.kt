package org.misarch.experimentexecutor.controller.model.dto

data class BatchUpdateVariableDto(
    val variables: List<ServiceConfiguration.ConfigurationVariable>
)
