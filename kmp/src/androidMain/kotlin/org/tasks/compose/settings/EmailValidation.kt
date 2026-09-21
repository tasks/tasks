package org.tasks.compose.settings

import android.util.Patterns

internal actual fun isValidEmail(email: String): Boolean =
    Patterns.EMAIL_ADDRESS.matcher(email).matches()
