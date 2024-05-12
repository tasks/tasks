package org.tasks.activities

import com.google.api.services.tasks.model.TaskList
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.entity.CaldavCalendar
import org.tasks.googleapis.InvokerFactory
import org.tasks.ui.CompletableViewModel
import javax.inject.Inject

@HiltViewModel
class RenameListViewModel @Inject constructor(
        private val invoker: InvokerFactory) : CompletableViewModel<TaskList>() {
    suspend fun renameList(list: CaldavCalendar, name: String) {
        run { invoker.getGtasksInvoker(list.account!!).renameGtaskList(list.uuid, name)!! }
    }
}