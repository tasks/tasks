package org.tasks.etebase

import java.io.File
import java.security.MessageDigest

/** Leave legacy caches intact: calendar tokens still reference their cached objects. */
internal object EtebaseCacheLocation {
    fun root(filesDir: String, accountScope: String?): String {
        if (accountScope == null) return filesDir
        require(accountScope.isNotBlank()) { "Missing Etebase account scope" }
        val encoded = MessageDigest.getInstance("SHA-256")
            .digest(accountScope.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(File(filesDir, "etebase-accounts"), encoded).path
    }

    fun key(filesDir: String, username: String): Pair<String, String> =
        File(filesDir).absolutePath to username
}
