package org.tasks.data

@Deprecated("Only used for backup migration")
data class GoogleTaskAccount(
    var account: String? = null,
    var etag: String? = null,
    var isCollapsed: Boolean = false,
)
