package com.todoroo.astrid.adapter

import org.tasks.data.TaskContainer

interface TaskAdapterDataSource {
    fun getItem(position: Int): TaskContainer?

    fun getTaskCount(): Int

    fun isHeader(position: Int): Boolean = false

    fun nearestHeader(position: Int): Long = -1

    val sortMode: Int get() = -1

    val subtaskSortMode: Int get() = -1
}