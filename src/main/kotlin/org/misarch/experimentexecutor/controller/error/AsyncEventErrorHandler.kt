package org.misarch.experimentexecutor.controller.error

import org.springframework.stereotype.Component
import reactor.core.publisher.FluxSink
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class AsyncEventErrorHandler(
    private val eventEmitters: ConcurrentHashMap<String, FluxSink<String>>
) {
    fun handleError(testUUID: UUID, testVersion: String, errorMessage: String) {
        val key = "$testUUID:$testVersion"
        val eventSink = eventEmitters[key]
        eventSink?.next("Error occurred during experiment execution: $errorMessage")
        eventEmitters.remove(key)
    }
}