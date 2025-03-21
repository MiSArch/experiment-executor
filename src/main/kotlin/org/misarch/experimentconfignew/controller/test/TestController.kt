package org.misarch.experimentconfignew.controller.test

import org.misarch.experimentconfignew.service.GraphQLQueryGeneratorService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
class TestController(
    private val graphQLQueryGeneratorService: GraphQLQueryGeneratorService,
) {
    @PostMapping("/test")
    suspend fun createLayout() {
        val response = graphQLQueryGeneratorService.generateGraphQL()
    }
}