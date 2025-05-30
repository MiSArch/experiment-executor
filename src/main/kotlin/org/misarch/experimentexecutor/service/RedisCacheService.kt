package org.misarch.experimentexecutor.service

import org.misarch.experimentexecutor.service.model.ExperimentState
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.getAndAwait
import org.springframework.data.redis.core.setAndAwait
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class RedisCacheService(
    private val experimentStateRedisTemplate: ReactiveRedisTemplate<String, ExperimentState>,
) {
    suspend fun cacheExperimentState(
        experimentState: ExperimentState
    ) {
        experimentStateRedisTemplate.opsForValue().setAndAwait(experimentState.testUUID.toString(), experimentState, Duration.ofDays(1))
    }

    suspend fun retrieveExperimentState(
        key: UUID,
    ): ExperimentState {
        return experimentStateRedisTemplate.opsForValue().getAndAwait(key.toString()) ?: throw IllegalStateException("Experiment state not found for key: $key")
    }
}