package org.tasks.logging

import kotlinx.datetime.TimeZone
import org.tasks.kmp.org.tasks.time.currentLocaleTag

fun debugInfo(): String = """
    ${startupMessage()}
    Locale: ${currentLocaleTag()}
    Timezone: ${TimeZone.currentSystemDefault().id}
""".trimIndent()
