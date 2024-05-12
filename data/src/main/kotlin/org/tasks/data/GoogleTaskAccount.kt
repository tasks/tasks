package org.tasks.data

import kotlinx.serialization.Serializable

@Deprecated("Only used for backup migration")
@Serializable
data class GoogleTaskAccount(
    var account: String? = null,
    var etag: String? = null,
    var isCollapsed: Boolean = false,
)
