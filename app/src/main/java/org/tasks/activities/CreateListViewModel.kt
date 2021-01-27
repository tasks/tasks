package org.tasks.activities

import com.google.api.services.tasks.model.TaskList
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.googleapis.InvokerFactory
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@HiltViewModel
class CreateListViewModel @Inject constructor(
        private val invoker: InvokerFactory
) : CompletableViewModel<TaskList>() {
    suspend fun createList(account: String, name: String) {
        run { invoker.getGtasksInvoker(account).createGtaskList(name)!! }
    }
}