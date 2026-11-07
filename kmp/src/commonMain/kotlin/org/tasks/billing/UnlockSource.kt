package org.tasks.billing

import kotlinx.serialization.Serializable

@Serializable
enum class UnlockSource {
    GOOGLE_PLAY,
    GITHUB,
    STRIPE,
}

val EntitlementProvider.unlockSource: UnlockSource?
    get() = when (this) {
        EntitlementProvider.GITHUB_SPONSOR -> UnlockSource.GITHUB
        EntitlementProvider.STRIPE -> UnlockSource.STRIPE
        EntitlementProvider.PLAY -> UnlockSource.GOOGLE_PLAY
    }
