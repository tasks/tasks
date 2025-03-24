package org.tasks.extensions

import android.content.Intent

const val FLAG_FROM_HISTORY
        = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY

val Intent.isFromHistory: Boolean
    get() = flags and FLAG_FROM_HISTORY == FLAG_FROM_HISTORY

val Intent.broughtToFront: Boolean
    get() = flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT > 0

val Intent.flagsToString
    get() = Intent::class.java.declaredFields
        .filter { it.name.startsWith("FLAG_") }
        .filter { flags or it.getInt(null) == flags }
        .joinToString(" | ") { it.name }