package org.tasks.activities

import androidx.hilt.lifecycle.ViewModelInject
import com.google.api.services.tasks.model.TaskList
import com.todoroo.astrid.gtasks.api.GtasksInvoker
import org.tasks.data.GoogleTaskList
import org.tasks.ui.CompletableViewModel

class RenameListViewModel @ViewModelInject constructor(
        private val invoker: GtasksInvoker) : CompletableViewModel<TaskList>() {
    suspend fun renameList(list: GoogleTaskList, name: String) {
        run { invoker.forAccount(list.account!!).renameGtaskList(list.remoteId, name)!! }
    }
}