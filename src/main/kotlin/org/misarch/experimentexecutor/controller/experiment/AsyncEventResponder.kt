package org.misarch.experimentexecutor.controller.experiment

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Configuration
class EventEmitterConfig {
    @Bean
    fun eventEmitters(): ConcurrentHashMap<String, FluxSink<String>> = ConcurrentHashMap()
}

/**
 * Controller for handling experiment events.
 * This controller allows clients to register for real-time updates on experiment events.
 *
 * @param eventEmitters A thread-safe map that holds the FluxSink instances for each experiment identified by testUUID and testVersion.
 */
@RestController
class EventController(
    private val eventEmitters: ConcurrentHashMap<String, FluxSink<String>>,
) {
    @GetMapping("/experiment/{testUUID}/{testVersion}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    private fun registerEvent(
        @PathVariable testUUID: UUID,
        @PathVariable testVersion: String,
    ): Flux<String> {
        val key = "$testUUID:$testVersion"
        return Flux.create { sink ->
            eventEmitters[key] = sink
            sink.onDispose { eventEmitters.remove(key) }
        }
    }
}

@Component
class AsyncEventResponder(
    private val eventEmitters: ConcurrentHashMap<String, FluxSink<String>>,
) {
    fun emitSuccess(
        testUUID: UUID,
        testVersion: String,
    ) {
        val key = "$testUUID:$testVersion"
        val eventSink = eventEmitters[key]
        eventSink?.next("http://localhost:3001/d/$testUUID-$testVersion")
        eventEmitters.remove(key)
    }
}

@Component
class AsyncEventErrorHandler(
    private val eventEmitters: ConcurrentHashMap<String, FluxSink<String>>,
) {
    fun handleError(
        testUUID: UUID,
        testVersion: String,
        errorMessage: String,
    ) {
        val key = "$testUUID:$testVersion"
        val eventSink = eventEmitters[key]
        eventSink?.next("Error occurred during experiment execution: $errorMessage")
        eventEmitters.remove(key)
    }
}
