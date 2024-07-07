package org.tasks.kmp

import org.tasks.extensions.formatNumber
import java.util.Locale

actual fun formatNumber(number: Int) = Locale.getDefault().formatNumber(number)