package org.misarch.experimentexecutor.service.event

import org.springframework.stereotype.Service

@Service
class EventService {

    suspend fun heartbeat(serviceName: String, replicaId: String) {
        // TODO implement
    }
}