package org.misarch.experimentconfignew.controller.model.dto

data class HeartbeatSubscribeDto (
    val topic: String,
    val route: String,
    val pubsubName: String,
)