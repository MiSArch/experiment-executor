package org.misarch.experimentconfignew.controller.test

import org.misarch.experimentconfignew.service.test.GraphQLQueryGeneratorService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(
    private val graphQLQueryGeneratorService: GraphQLQueryGeneratorService,
) {
    @PostMapping("/test")
    suspend fun createLayout() {
        graphQLQueryGeneratorService.generateGraphQL()
    }
}