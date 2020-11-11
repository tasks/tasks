package org.tasks.activities

import androidx.hilt.lifecycle.ViewModelInject
import com.google.api.services.tasks.model.TaskList
import org.tasks.googleapis.InvokerFactory
import org.tasks.ui.CompletableViewModel

class CreateListViewModel @ViewModelInject constructor(
        private val invoker: InvokerFactory
) : CompletableViewModel<TaskList>() {
    suspend fun createList(account: String, name: String) {
        run { invoker.getGtasksInvoker(account).createGtaskList(name)!! }
    }
}