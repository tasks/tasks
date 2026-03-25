package org.tasks.watch

sealed class WatchListItem {
    data class Header(
        val id: String,
        val title: String,
    ) : WatchListItem()

    data class FilterItem(
        val id: String,
        val title: String,
        val icon: String?,
        val color: Int,
        val textColor: Int,
        val taskCount: Int,
    ) : WatchListItem()
}
