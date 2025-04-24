package org.tasks.compose

import kotlinx.serialization.Serializable

@Serializable
object HomeDestination

@Serializable
data class AddAccountDestination(val showImport: Boolean)
