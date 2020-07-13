package org.tasks.activities

import com.google.api.services.tasks.model.TaskList
import com.todoroo.astrid.gtasks.api.GtasksInvoker
import org.tasks.ui.CompletableViewModel

class CreateListViewModel : CompletableViewModel<TaskList?>() {
    fun createList(invoker: GtasksInvoker, account: String?, name: String?) {
        run { invoker.forAccount(account!!).createGtaskList(name) }
    }
}