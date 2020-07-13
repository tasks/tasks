package org.tasks.activities

import com.google.api.services.tasks.model.TaskList
import com.todoroo.astrid.gtasks.api.GtasksInvoker
import org.tasks.data.GoogleTaskList
import org.tasks.ui.CompletableViewModel

class RenameListViewModel : CompletableViewModel<TaskList?>() {
    fun renameList(invoker: GtasksInvoker, list: GoogleTaskList, name: String?) {
        run { invoker.forAccount(list.account!!).renameGtaskList(list.remoteId, name) }
    }
}