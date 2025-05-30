package org.misarch.experimentexecutor.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.misarch.experimentexecutor.service.model.ExperimentState
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer

@Configuration
class RedisConfig {
    @Bean
    fun reactiveRedisTemplate(factory: LettuceConnectionFactory, objectMapper: ObjectMapper): ReactiveRedisTemplate<String, ExperimentState> {
        val serializer = Jackson2JsonRedisSerializer(objectMapper.findAndRegisterModules(), ExperimentState::class.java)

        val context: RedisSerializationContext<String, ExperimentState> =
            RedisSerializationContext
                .newSerializationContext<String, ExperimentState>(RedisSerializer.string())
                .value(serializer)
                .build()

        return ReactiveRedisTemplate(factory, context)
    }
}