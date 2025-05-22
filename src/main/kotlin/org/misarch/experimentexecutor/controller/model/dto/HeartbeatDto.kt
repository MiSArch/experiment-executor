package org.misarch.experimentexecutor.controller.model.dto

data class HeartbeatDto(
    val serviceName: String,
    val replicaId: String,
)