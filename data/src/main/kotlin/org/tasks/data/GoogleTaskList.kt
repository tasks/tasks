package org.tasks.data

@Deprecated("Only used for backup migration")
data class GoogleTaskList(
    var account: String? = null,
    var remoteId: String? = null,
    var title: String? = null,
    var order: Int = NO_ORDER,
    var lastSync: Long = 0,
    var color: Int? = null,
    var icon: Int? = -1,
)