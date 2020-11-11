package org.tasks.activities

import androidx.hilt.lifecycle.ViewModelInject
import com.google.api.services.tasks.model.TaskList
import org.tasks.data.GoogleTaskList
import org.tasks.googleapis.InvokerFactory
import org.tasks.ui.CompletableViewModel

class RenameListViewModel @ViewModelInject constructor(
        private val invoker: InvokerFactory) : CompletableViewModel<TaskList>() {
    suspend fun renameList(list: GoogleTaskList, name: String) {
        run { invoker.getGtasksInvoker(list.account!!).renameGtaskList(list.remoteId, name)!! }
    }
}