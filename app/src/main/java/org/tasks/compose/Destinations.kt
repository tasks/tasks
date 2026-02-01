package org.tasks.compose

import kotlinx.serialization.Serializable

@Serializable
object HomeDestination

@Serializable
object WelcomeDestination

@Serializable
object AddAccountDestination

@Serializable
data class PurchaseDestination(
    val nameYourPrice: Boolean = true,
    val github: Boolean = false,
    val feature: Int = 0,
)
