package org.tasks.data.entity

enum class OpenTaskProvider(
    val accountType: String,
    val packageName: String,
    val displayName: String,
    val syncType: String,
) {
    DAVX5("bitfire.at.davdroid", "at.bitfire.davdroid", "DAVx\u2075", "davx5"),
    DAVX5_MANAGED("com.davdroid", "com.davdroid", "DAVx\u2075", "davx5_managed"),
    ETESYNC("com.etesync.syncadapter", "com.etesync.syncadapter", "EteSync", "etesync_ot"),
    DECSYNC("org.decsync.tasks", "org.decsync.cc", "DecSync CC", "decsync"),
    KSYNC("infomaniak.com.sync", "com.infomaniak.sync", "kSync", "ksync"),
    ;

    companion object {
        fun fromUuid(uuid: String?) = entries.find { uuid?.startsWith(it.accountType) == true }

        val SUPPORTED_TYPES = entries.map { it.accountType }.toSet()

        val SUPPORTED_TYPE_FILTER =
            SUPPORTED_TYPES.joinToString(" OR ") { "ACCOUNT_TYPE = '$it'" }
    }
}
