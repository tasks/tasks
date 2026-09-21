package org.tasks.kmp.org.tasks.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.FormatStyle

fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun DateStyle.toFormatStyle() = when (this) {
    DateStyle.FULL -> FormatStyle.FULL
    DateStyle.LONG -> FormatStyle.LONG
    DateStyle.MEDIUM -> FormatStyle.MEDIUM
    DateStyle.SHORT -> FormatStyle.SHORT
}
