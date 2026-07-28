package org.tasks.extensions

import java.text.NumberFormat
import java.util.Locale

actual fun localizedNumber(number: Int): String =
    NumberFormat.getNumberInstance(Locale.getDefault())
        .apply { isGroupingUsed = false }
        .format(number.toLong())
