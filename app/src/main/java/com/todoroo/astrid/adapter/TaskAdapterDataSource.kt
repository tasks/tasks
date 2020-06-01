package com.todoroo.astrid.adapter

import org.tasks.data.TaskContainer

interface TaskAdapterDataSource {
    fun getItem(position: Int): TaskContainer?

    fun getTaskCount(): Int

    fun isHeader(position: Int): Boolean = false
}