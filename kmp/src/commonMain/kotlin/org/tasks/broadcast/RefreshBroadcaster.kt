package org.tasks.broadcast

interface RefreshBroadcaster {
    fun broadcastRefresh()
    fun broadcastTaskCompleted(ids: List<Long>, oldDueDate: Long = 0L) {}
}
