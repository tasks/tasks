package org.tasks.activities

import androidx.hilt.lifecycle.ViewModelInject
import com.google.api.services.tasks.model.TaskList
import com.todoroo.astrid.gtasks.api.GtasksInvoker
import org.tasks.ui.CompletableViewModel

class CreateListViewModel @ViewModelInject constructor(
        private val invoker: GtasksInvoker) : CompletableViewModel<TaskList>() {
    fun createList(account: String, name: String) {
        run { invoker.forAccount(account).createGtaskList(name)!! }
    }
}