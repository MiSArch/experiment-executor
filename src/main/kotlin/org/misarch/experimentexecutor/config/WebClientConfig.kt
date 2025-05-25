package org.misarch.experimentexecutor.config

import io.netty.channel.ChannelOption
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class WebClientConfig {

    @Bean
    fun httpClient(): HttpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
        .responseTimeout(Duration.ofMillis(10_000))

    @Bean("webClient")
    fun webClient(httpClient: HttpClient) =
        WebClient.builder().clientConnector(ReactorClientHttpConnector(httpClient)).build()
}