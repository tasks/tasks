package org.tasks.activities

import androidx.hilt.lifecycle.ViewModelInject
import com.todoroo.astrid.gtasks.api.GtasksInvoker
import org.tasks.data.GoogleTaskList
import org.tasks.ui.ActionViewModel

class DeleteListViewModel @ViewModelInject constructor(
        private val invoker: GtasksInvoker) : ActionViewModel() {
    fun deleteList(list: GoogleTaskList) {
        run { invoker.forAccount(list.account!!).deleteGtaskList(list.remoteId) }
    }
}