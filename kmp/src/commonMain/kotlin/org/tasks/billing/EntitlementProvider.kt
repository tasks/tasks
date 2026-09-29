package org.tasks.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EntitlementProvider {
    @SerialName("play") PLAY,
    @SerialName("github_sponsor") GITHUB_SPONSOR,
}
