package org.misarch.experimentexecutor.plugin.failure.misarch

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay

private val logger = KotlinLogging.logger {}

suspend fun <T> withRetries(
    maxRetries: Int = 3,
    initialDelayMillis: Long = 500,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelayMillis
    repeat(maxRetries - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            logger.warn { "Attempt ${attempt + 1} failed: ${e.message}, retrying in $currentDelay ms" }
            delay(currentDelay)
            currentDelay *= 2
        }
    }
    return block()
}
