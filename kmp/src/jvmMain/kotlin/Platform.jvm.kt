package org.tasks.kmp

import java.util.Locale

actual fun formatNumber(number: Int): String =
    java.text.NumberFormat.getIntegerInstance(Locale.getDefault()).format(number)

actual val PROD_ID = "+//IDN tasks.org//desktop-${JvmBuildConfig.VERSION_CODE}//EN"

actual val DEV_URL: String = JvmBuildConfig.DEV_URL

