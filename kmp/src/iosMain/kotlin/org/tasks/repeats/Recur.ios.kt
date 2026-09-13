package org.tasks.repeats

import org.tasks.time.DateTime

internal actual fun parseRecur(rrule: String): Recur = TODO("libical")

internal actual fun serializeRecur(recur: Recur): String = TODO("libical")

actual fun Recur.nextOccurrence(start: DateTime, hasTime: Boolean): DateTime? = TODO("libical")
