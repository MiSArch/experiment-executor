package org.misarch.experimentexecutor.controller.event

import org.misarch.experimentexecutor.service.event.EventService
import org.misarch.experimentexecutor.controller.model.dto.HeartbeatDto
import org.misarch.experimentexecutor.controller.model.dto.HeartbeatSubscribeDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class EventController(
    private val eventService: EventService,
) {

    companion object {
        private const val TOPIC = "heartbeat"
        private const val ROUTE = "heartbeat"
        private const val PUBSUB_NAME = "experiment-config-pubsub"
    }

    @PostMapping("/heartbeat")
    @ResponseStatus(code = HttpStatus.OK)
    suspend fun heartbeat(event: HeartbeatDto) {
        eventService.heartbeat(event.serviceName, event.replicaId)
    }

    @GetMapping("/dapr/subscribe")
    @ResponseStatus(code = HttpStatus.OK)
    suspend fun subscribe(): List<HeartbeatSubscribeDto> {
        return listOf(
            HeartbeatSubscribeDto(
                topic = TOPIC,
                route = ROUTE,
                pubsubName = PUBSUB_NAME,
            )
        )
    }
}