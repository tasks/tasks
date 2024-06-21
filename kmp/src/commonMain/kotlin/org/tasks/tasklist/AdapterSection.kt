package org.tasks.tasklist

data class AdapterSection(
        var firstPosition: Int,
        val value: Long,
        var sectionedPosition: Int = 0,
        var collapsed: Boolean = false
)