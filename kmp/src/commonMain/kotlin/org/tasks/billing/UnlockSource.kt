package org.tasks.billing

import kotlinx.serialization.Serializable

@Serializable
enum class UnlockSource {
    GOOGLE_PLAY,
    GITHUB,
    STRIPE,
}
