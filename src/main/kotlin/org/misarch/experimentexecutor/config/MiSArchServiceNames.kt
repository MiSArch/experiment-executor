package org.misarch.experimentexecutor.config

// TODO keycloak is missing yet, but it has no proper metrics yet
// frontend is missing deliberately as it is usually not part of an experiment
val MISARCH_SERVICES =
    listOf(
        "address",
        "catalog",
        "discount",
        "gateway",
        "inventory",
        "invoice",
        "media",
        "notification",
        "order",
        "payment",
        "return",
        "review",
        "shipment",
        "shoppingcart",
        "simulation",
        "tax",
        "user",
        "wishlist",
    )
