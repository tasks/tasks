package org.tasks.activities

import androidx.hilt.lifecycle.ViewModelInject
import org.tasks.data.GoogleTaskList
import org.tasks.googleapis.InvokerFactory
import org.tasks.ui.ActionViewModel

class DeleteListViewModel @ViewModelInject constructor(
        private val invoker: InvokerFactory) : ActionViewModel() {
    suspend fun deleteList(list: GoogleTaskList) {
        run { invoker.getGtasksInvoker(list.account!!).deleteGtaskList(list.remoteId) }
    }
}