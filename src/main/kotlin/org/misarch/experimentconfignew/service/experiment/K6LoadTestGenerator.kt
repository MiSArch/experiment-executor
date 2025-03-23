package org.misarch.experimentconfignew.service.experiment

import org.misarch.experimentconfignew.executor.model.*
import org.springframework.stereotype.Service

@Service
class K6LoadTestGenerator(
    private val graphQLQueryGeneratorService: GraphQLQueryGeneratorService
) {

    suspend fun generateLoadTest(load: Load): String {
        if (load.graphql != null) {
            val graphQLQueriesAndMutations = graphQLQueryGeneratorService.generateGraphQL(load.graphql.uri)
            load.graphql.flows.forEach { flow ->

                val orderedRequests = mutableListOf<Request>()
                flow.requests.forEach { request ->
                    request.orderRequestsByInputs(flow, orderedRequests)
                }
                orderedRequests.forEach { request ->
                    // TODO how to handle requests without inputs?
                    // TODO implement oauth flow
                    val inputVariable = request.inputs?.first()?.let { input  ->
                    if (input.refInput != null && input.refInput.list?.isNotEmpty() == true) {
                            "${input.refInput.name}_${input.refInput.list.first().first}_${(input.refInput).list.first().second}"
                        } else null
                    }
                    val queryOrMutation = if (inputVariable != null) {
                        graphQLQueriesAndMutations.getValue(request.name).replace("UUID", inputVariable)
                    } else {
                        graphQLQueriesAndMutations.getValue(request.name)
                    }
                    val k6Request = """
                        const ${request.name} = `${queryOrMutation}`
                        const ${request.name}_payload = JSON.stringify({query: ${request.name}});
                        const ${request.name}_params = {
                            headers: {
                                'Content-Type': 'application/json',
                                'Authorization': `Bearer ${"$"}{BEARER_TOKEN}`
                            }
                        };
                        const ${request.name}_res = http.post(GRAPHQL_ENDPOINT, ${request.name}_payload, ${request.name}_params);
                    """.trimIndent()

                    println(k6Request)
                }
            }

        }

        return "TODO this will be the uri of the load test"
    }

    private suspend fun Request.orderRequestsByInputs(flow: Flow, orderedRequests: MutableList<Request>) {
        inputs?.forEach { input ->
            if (input.refInput != null) {
                    println(input.refInput.name)
                    val predecessingRequest = flow.requests.first { input.refInput == it.outputs?.first { output -> output.name == input.refInput.name} }
                    predecessingRequest.orderRequestsByInputs(flow, orderedRequests)
                }
            }
        if (orderedRequests.contains(this)) {
            orderedRequests.remove(this)
        }
        orderedRequests.add(this)
    }

    private suspend fun generateK6Script(queries: Map<String, String>, load: Load): String {
        return """
            import http from 'k6/http';
            import { check, sleep } from 'k6';

            const GRAPHQL_ENDPOINT = '${load.graphql?.uri}';

            export let options = {
              vus: 10,
              duration: '30s',
            };

            const queries = ${queries.values.joinToString(",\n") { "\"$it\"" }};

            export default function () {
                let randomQuery = queries[Math.floor(Math.random() * queries.length)];
                let payload = JSON.stringify({ query: randomQuery });

                let params = { headers: { 'Content-Type': 'application/json' } };
                let res = http.post(GRAPHQL_ENDPOINT, payload, params);

                check(res, {
                    'is status 200': (r) => r.status === 200,
                    'response time < 500ms': (r) => r.timings.duration < 500,
                });

                sleep(1);
            }
        """.trimIndent()
    }

    suspend fun executeLoadTest(loadTestUri: String) {
        // TODO execute the load test
    }
}