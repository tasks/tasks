package org.tasks.kmp

import org.tasks.extensions.toLocale

actual fun formatNumber(number: Int, languageTag: String?): String =
    java.text.NumberFormat.getIntegerInstance(languageTag.toLocale()).format(number)

actual val PROD_ID = "+//IDN tasks.org//desktop-${JvmBuildConfig.VERSION_CODE}//EN"

actual val DEV_URL: String = JvmBuildConfig.DEV_URL

