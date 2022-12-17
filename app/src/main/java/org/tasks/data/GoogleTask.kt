package org.tasks.data

@Deprecated("For backup use only")
data class GoogleTask(
    var remoteId: String? = "",
    var listId: String? = "",
    var remoteParent: String? = null,
    var remoteOrder: Long = 0,
    var lastSync: Long = 0,
    var deleted: Long = 0,
) {
    companion object {
        const val KEY = "gtasks"
    }
}