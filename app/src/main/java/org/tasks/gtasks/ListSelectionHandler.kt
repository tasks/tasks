package org.tasks.gtasks

import com.todoroo.astrid.api.Filter

interface ListSelectionHandler {
    fun addAccount()

    fun selectedList(list: Filter?)
}