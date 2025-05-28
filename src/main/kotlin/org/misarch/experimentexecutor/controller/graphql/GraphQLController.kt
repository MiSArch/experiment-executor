package org.misarch.experimentexecutor.controller.graphql

import org.misarch.experimentexecutor.service.GraphQLQueryGeneratorService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
class ExperimentController(
    private val graphQLQueryGeneratorService: GraphQLQueryGeneratorService,
) {
    @GetMapping("/generateGraphQL")
    suspend fun createLayout(@RequestParam graphQLURL: String) {
        graphQLQueryGeneratorService.generateGraphQL(URI(graphQLURL))
    }
}