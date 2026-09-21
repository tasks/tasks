package org.tasks.compose.settings

// Ported from Android's Patterns.EMAIL_ADDRESS (AOSP PatternsCompat)
private val EMAIL_ADDRESS_PATTERN = Regex(
    "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
)

internal actual fun isValidEmail(email: String): Boolean =
    EMAIL_ADDRESS_PATTERN.matches(email)
