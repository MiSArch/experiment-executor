package org.misarch.experimentexecutor.controller.model.dto

data class HeartbeatSubscribeDto (
    val topic: String,
    val route: String,
    val pubsubName: String,
)