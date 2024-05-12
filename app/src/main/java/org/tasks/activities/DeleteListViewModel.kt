package org.tasks.activities

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.entity.CaldavCalendar
import org.tasks.googleapis.InvokerFactory
import org.tasks.ui.ActionViewModel
import javax.inject.Inject

@HiltViewModel
class DeleteListViewModel @Inject constructor(
        private val invoker: InvokerFactory) : ActionViewModel() {
    suspend fun deleteList(list: CaldavCalendar) {
        run { invoker.getGtasksInvoker(list.account!!).deleteGtaskList(list.uuid) }
    }
}