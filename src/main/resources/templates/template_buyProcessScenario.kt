package org.misarch

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.http
import java.time.Duration

val buyProcessScenario = scenario("Buy Process").exec { session ->
    session.set(
        "productsQuery",
        "{ \"query\": \"query { products(filter: { isPubliclyVisible: true }, first: 10, orderBy: { direction: ASC, field: ID }, skip: 0) { hasNextPage nodes { id internalName isPubliclyVisible } totalCount } }\" }"
    )
}.exec(
    http("products").post("/graphql").body(StringBody("#{productsQuery}")).check(jsonPath("$.data.products.nodes[0].id").saveAs("productId"))
).pause(Duration.ofMillis(4000), Duration.ofMillis(10000)).exec { session ->
    val productId = session.getString("productId")
    session.set(
        "productQuery",
        "{ \"query\": \"query { product(id: \\\"$productId\\\") { categories { hasNextPage  totalCount } defaultVariant { id isPubliclyVisible averageRating } id internalName isPubliclyVisible variants { hasNextPage  totalCount } } }\" }"
    )
}.exec(
    http("product").post("/graphql").body(StringBody("#{productQuery}"))
        .check(jsonPath("$.data.product.defaultVariant.id").saveAs("productVariantId"))
).pause(Duration.ofMillis(50), Duration.ofMillis(150)).exec { session ->
    session.set(
        "usersQuery",
        "{ \"query\": \"query { users(first: 10, orderBy: { direction: ASC, field: ID }, skip: 0) { hasNextPage nodes {  id  birthday dateJoined gender username addresses { nodes { id } } } } }\" }"
    )
}.exec(
    http("users").post("/graphql").body(StringBody("#{usersQuery}"))
        .check(jsonPath("$.data.users.nodes[0].addresses.nodes[0].id").saveAs("addressId"))
        .check(jsonPath("$.data.users.nodes[0].id").saveAs("userId"))
).pause(Duration.ofMillis(50), Duration.ofMillis(150)).exec { session ->
    val userId = session.getString("userId")
    val productVariantId = session.getString("productVariantId")
    session.set(
        "createShoppingcartItemMutation",
        "{ \"query\": \"mutation { createShoppingcartItem( input: { id: \\\"$userId\\\" shoppingCartItem: { count: 1 productVariantId: \\\"$productVariantId\\\" } } ) { id } }\" }"
    )
}.exec(
    http("createShoppingcartItemMutation").post("/graphql").body(StringBody("#{createShoppingcartItemMutation}"))
        .check(jsonPath("$.data.createShoppingcartItem.id").saveAs("createShoppingcartItemId"))
).pause(Duration.ofMillis(4000), Duration.ofMillis(7000)).exec { session ->
    session.set(
        "shipmentMethodsQuery",
        "{ \"query\": \"query { shipmentMethods { totalCount nodes { id name baseFees description feesPerItem feesPerKg } } }\" }",
    )
}.exec(
    http("shipmentMethodsQuery").post("/graphql").body(StringBody("#{shipmentMethodsQuery}"))
        .check(jsonPath("$.data.shipmentMethods.nodes[0].id").saveAs("shipmentMethodId"))
).exec { session ->
    session.set(
        "paymentInformationsQuery",
        "{ \"query\": \"query { paymentInformations { nodes { id paymentMethod publicMethodDetails payments { nodes { id status totalAmount numberOfRetries payedAt } } } } }\" }",
    )
}.exec(
    http("paymentInformationsQuery").post("/graphql").body(StringBody("#{paymentInformationsQuery}"))
        .check(jsonPath("$.data.paymentInformations.nodes[0].id").saveAs("paymentInformationId"))
).pause(Duration.ofMillis(4000), Duration.ofMillis(7000)).exec { session ->
    val userId = session.getString("userId")
    val addressId = session.getString("addressId")
    val createShoppingcartItemId = session.getString("createShoppingcartItemId")
    val shipmentMethodId = session.getString("shipmentMethodId")
    val paymentInformationId = session.getString("paymentInformationId")
    session.set(
        "createOrderMutation",
        "{ \"query\": \"mutation { createOrder( input: { userId: \\\"$userId\\\" orderItemInputs: { shoppingCartItemId: \\\"$createShoppingcartItemId\\\" couponIds: [] shipmentMethodId: \\\"$shipmentMethodId\\\" } vatNumber: \\\"AB1234\\\" invoiceAddressId: \\\"$addressId\\\" shipmentAddressId: \\\"$addressId\\\" paymentInformationId: \\\"$paymentInformationId\\\" } ) { id paymentInformationId placedAt } }\" }"
    )
}.pause(Duration.ofMillis(2000), Duration.ofMillis(5000)).exec(
    http("createOrderMutation").post("/graphql").body(StringBody("#{createOrderMutation}"))
        .check(jsonPath("$.data.createOrder.id").saveAs("createOrderId"))
).exec { session ->
    val createOrderId = session.getString("createOrderId")
    session.set(
        "placeOrderMutation",
        "{ \"query\": \"mutation { placeOrder(input: { id: \\\"$createOrderId\\\", paymentAuthorization: { cvc: 123 } }) { id } }\" }"
    )
}.exec(
    http("placeOrderMutation").post("/graphql").body(StringBody("#{placeOrderMutation}"))
)