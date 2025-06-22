package org.misarch

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.http
import java.time.Duration

val abortedBuyProcessScenario = scenario("abortedBuyProcessScenario")
    .exec { session ->
        session.set("targetUrl", "http://gateway:8080/graphql")
    }
    .exec(
        http("Get Access Token")
            .post("http://keycloak:80/keycloak/realms/Misarch/protocol/openid-connect/token")
            .formParam("client_id", "frontend")
            .formParam("grant_type", "password")
            .formParam("username", "gatling")
            .formParam("password", "123")
            .check(jsonPath("$.access_token").saveAs("accessToken"))
    )
    .pause(Duration.ofMillis(0), Duration.ofMillis(0))
    .exec { session ->
        session.set(
            "productsQuery",
            "{ \"query\": \"query { products(filter: { isPubliclyVisible: true }, first: 10, orderBy: { direction: ASC, field: ID }, skip: 0) { hasNextPage nodes { id internalName isPubliclyVisible } totalCount } }\" }"
        )
    }
    .exec(
        http("products").post("#{targetUrl}").header("Content-Type", "application/json").header("Authorization", "Bearer #{accessToken}").body(StringBody("#{productsQuery}"))
            .check(jsonPath("$.data.products.nodes[0].id").saveAs("productId"))
    )
    .pause(Duration.ofMillis(4000), Duration.ofMillis(10000))
    .exec { session ->
        val productId = session.getString("productId")
        session.set(
            "productQuery",
            "{ \"query\": \"query { product(id: \\\"$productId\\\") { categories { hasNextPage  totalCount } defaultVariant { id isPubliclyVisible averageRating } id internalName isPubliclyVisible variants { hasNextPage  totalCount } } }\" }"
        )
    }
    .exec(
        http("product").post("#{targetUrl}").header("Content-Type", "application/json").header("Authorization", "Bearer #{accessToken}").body(StringBody("#{productQuery}"))
            .check(jsonPath("$.data.product.defaultVariant.id").saveAs("productVariantId"))
    )
    .pause(Duration.ofMillis(50), Duration.ofMillis(150))
    .exec { session ->
        session.set(
            "usersQuery",
            "{ \"query\": \"query { users(first: 10, orderBy: { direction: ASC, field: ID }, skip: 0) { hasNextPage nodes {  id  birthday dateJoined gender username addresses { nodes { id } } } } }\" }"
        )
    }
    .exec(
        http("users").post("#{targetUrl}").header("Content-Type", "application/json").header("Authorization", "Bearer #{accessToken}").body(StringBody("#{usersQuery}"))
            .check(jsonPath("$.data.users.nodes[0].addresses.nodes[0].id").saveAs("addressId"))
            .check(jsonPath("$.data.users.nodes[0].id").saveAs("userId"))
    )
    .pause(Duration.ofMillis(50), Duration.ofMillis(150))
    .exec { session ->
        val userId = session.getString("userId")
        val productVariantId = session.getString("productVariantId")
        session.set(
            "createShoppingcartItemMutation",
            "{ \"query\": \"mutation { createShoppingcartItem( input: { id: \\\"$userId\\\" shoppingCartItem: { count: 1 productVariantId: \\\"$productVariantId\\\" } } ) { id } }\" }"
        )
    }
    .exec(
        http("createShoppingcartItemMutation").post("#{targetUrl}").header("Content-Type", "application/json").header("Authorization", "Bearer #{accessToken}").body(StringBody("#{createShoppingcartItemMutation}"))
            .check(jsonPath("$.data.createShoppingcartItem.id").saveAs("createShoppingcartItemId"))
    )
    .pause(Duration.ofMillis(0), Duration.ofMillis(0))