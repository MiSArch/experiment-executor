package org.misarch.experimentexecutor.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.io.File
import java.net.URI

private val logger = KotlinLogging.logger {}

@Suppress("UNCHECKED_CAST")
@Service
class GraphQLQueryGeneratorService(
    private val webClient: WebClient
) {



    // TODO
    /*suspend fun graphQLQueryStringBuilder(request: Request, queryOrMutation: GraphQLRequest): String {
        queryOrMutation.inputs.forEach { queryInput -> request.i }
    }*/

    suspend fun generateGraphQL(graphQLURL: URI): Map<String, String> {
        try {
            val schema = fetchSchema(graphQLURL)
            val maxDepth = 2

            val queries = generateQueries(schema, maxDepth).toSortedMap()
            val mutations = generateMutations(schema, maxDepth).toSortedMap()

            File("requests.graphql").writeText("")
            queries.forEach { (name, query) ->
                File("requests.graphql").appendText("$name: $query\n\n")
            }
            mutations.forEach { (name, mutation) ->
                File("requests.graphql").appendText("$name: $mutation\n\n")
            }

            return queries + mutations

        } catch (error: Exception) {
            logger.error { "Error: ${error.message}" }
        }

        return emptyMap()
    }

    companion object {
        private const val QUERY = """
            {
                __schema {
                    types {
                        name
                        kind
                        fields {
                            name
                            args {
                                name
                                type {
                                    name
                                    kind
                                    ofType {
                                        name
                                        kind
                                        ofType {
                                            name
                                            kind
                                            ofType {
                                                name
                                                kind
                                            }
                                        }
                                    }
                                }
                            }
                            type {
                                name
                                kind
                                ofType {
                                    name
                                    kind
                                    ofType {
                                        name
                                        kind
                                        ofType {
                                            name
                                            kind
                                        }
                                    }
                                }
                            }
                        }
                        inputFields {
                            name
                            type {
                                name
                                kind
                                ofType {
                                    name
                                    kind
                                    ofType {
                                        name
                                        kind
                                        ofType {
                                            name
                                            kind
                                        }
                                    }
                                }
                            }
                        }
                        enumValues {
                            name
                        }
                    }
                }
            }
        """
    }

    private suspend fun fetchSchema(graphQLURL: URI): Map<String, Any> =
         withContext(Dispatchers.IO) {
             webClient.post()
                 .uri(graphQLURL)
                 .accept(MediaType.APPLICATION_JSON)
                 .bodyValue(mapOf("query" to QUERY))
                 .retrieve()
                 .bodyToMono(Map::class.java)
                 .block()
         } as Map<String, Any>


    private fun generateQueries(schema: Map<String, Any>, maxDepth: Int): Map<String, String> {
        val types = ((schema["data"] as Map<*, *>)["__schema"] as Map<*, *>)["types"] as List<Map<String, Any>>
        val queryTypeFields =
            types.find { it["name"] == "Query" }?.get("fields") as List<Map<String, Any>>? ?: return emptyMap()

        return queryTypeFields.createEntries(schema, maxDepth, "query")
    }

    private fun generateMutations(schema: Map<String, Any>, maxDepth: Int): Map<String, String> {
        val types = ((schema["data"] as Map<*, *>)["__schema"] as Map<*, *>)["types"] as List<Map<String, Any>>
        val mutationTypeFields =
            types.find { it["name"] == "Mutation" }?.get("fields") as List<Map<String, Any>>? ?: return emptyMap()

        return mutationTypeFields.createEntries(schema, maxDepth, "mutation")
    }

    private fun List<Map<String, Any>>.createEntries(schema: Map<String, Any>, maxDepth: Int, typeString: String): Map<String, String>  {
        return associate { element ->
            val args = (element["args"] as List<Map<String, Any>>).joinToString(", ") { arg ->
                "${arg["name"]}: ${getDefaultValue(arg["name"] as String, arg["type"] as Map<String, Any>, schema)}"
                // TODO the args should be added somehow to the request / query model
            }
            val fields = generateFields(element["type"] as Map<String, Any>, schema, maxDepth)
            element["name"] as String to if (args.isEmpty()) {
                "$typeString { ${element["name"]} { $fields } }"
            } else {
                "$typeString { ${element["name"]}($args) { $fields } }"
            }
        }
    }

    private fun generateFields(type: Map<String, Any>, schema: Map<String, Any>, depth: Int): String {
        if (depth == 0) return ""
        var fieldType = type
        while (fieldType["kind"] == "NON_NULL" || fieldType["kind"] == "LIST") {
            fieldType = fieldType["ofType"] as Map<String, Any>
        }

        val types = (schema["data"] as Map<*, *>)["__schema"] as Map<*, *>
        val typeFields = (types["types"] as List<Map<String, Any>>).find { it["name"] == fieldType["name"] }
            ?.get("fields") as List<Map<String, Any>>? ?: return ""

        return typeFields.joinToString(" ") { field ->
            fieldType = field["type"] as Map<String, Any>
            while (fieldType["kind"] == "NON_NULL" || fieldType["kind"] == "LIST") {
                fieldType = fieldType["ofType"] as Map<String, Any>
            }

            if (depth == 1 && fieldType["kind"] != "SCALAR" && fieldType["kind"] != "ENUM") {
                ""
            } else {
                val subFields = generateFields(fieldType, schema, depth - 1)
                if (subFields.isEmpty()) field["name"] as String else "${field["name"]} { $subFields }"
            }
        }
    }

    private fun getDefaultValue(name: String, type: Map<String, Any>, schema: Map<String, Any>): String {
        return when {
            type["kind"] == "NON_NULL" -> getDefaultValue(name, type["ofType"] as Map<String, Any>, schema)
            type["name"] == "Int" -> if (name == "first") "10" else "0"
            type["name"] == "Float" -> "1.0"
            type["name"] == "String" -> "\"example\""
            type["name"] == "Boolean" -> "true"
            type["kind"] == "ENUM" -> {
                val enumType = (schema["data"] as Map<*, *>)["__schema"] as Map<*, *>
                val types = enumType["types"] as List<Map<String, Any>>
                val enumValues = types.find { it["name"] == type["name"] }?.get("enumValues") as List<Map<String, Any>>?
                enumValues?.firstOrNull()?.get("name") as String? ?: "\"\""
            }

            type["kind"] == "INPUT_OBJECT" -> {
                val inputObjectType = (schema["data"] as Map<*, *>)["__schema"] as Map<*, *>
                val types = inputObjectType["types"] as List<Map<String, Any>>
                val inputFields =
                    types.find { it["name"] == type["name"] }?.get("inputFields") as List<Map<String, Any>>?
                inputFields?.joinToString(", ") { field ->
                    "${field["name"]}: ${
                        getDefaultValue(
                            field["name"] as String,
                            field["type"] as Map<String, Any>,
                            schema
                        )
                    }"
                }?.let { "{ $it }" } ?: "{}"
            }

            else -> "\"UUID\""
        }
    }
}