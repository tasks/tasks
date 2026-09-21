package org.tasks.activities

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity.Companion.EXTRA_CALDAV_ACCOUNT
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity.Companion.EXTRA_CALDAV_CALENDAR
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.googleapis.InvokerFactory
import org.tasks.service.TaskDeleter
import org.tasks.viewmodel.GoogleTaskListSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class GoogleTaskListSettingsHiltViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    caldavDao: CaldavDao,
    taskDeleter: TaskDeleter,
    reporting: Reporting,
    purchaseState: PurchaseState,
    invokerFactory: InvokerFactory,
    @ApplicationContext context: Context,
) : GoogleTaskListSettingsViewModel(
    caldavDao = caldavDao,
    taskDeleter = taskDeleter,
    reporting = reporting,
    purchaseState = purchaseState,
    invokerFactory = { account ->
        invokerFactory.getGtasksInvoker(account.username!!)
    },
    isDark = context.resources.getBoolean(R.bool.is_dark),
    account = savedStateHandle.get<CaldavAccount>(EXTRA_CALDAV_ACCOUNT)!!,
    calendar = savedStateHandle[EXTRA_CALDAV_CALENDAR]
        ?: CaldavCalendar(uuid = org.tasks.data.UUIDHelper.newUUID()),
    hasColorWheel = true,
) {
    init {
        savedStateHandle.get<String>(KEY_NAME)?.let { setName(it) }
        savedStateHandle.get<Int>(KEY_COLOR)?.let { setColor(it) }
        savedStateHandle.get<String>(KEY_ICON)?.let { setIcon(it) }
    }

    override fun setName(value: String) {
        super.setName(value)
        savedStateHandle[KEY_NAME] = value
    }

    override fun setColor(value: Int) {
        super.setColor(value)
        savedStateHandle[KEY_COLOR] = value
    }

    override fun setIcon(value: String) {
        super.setIcon(value)
        savedStateHandle[KEY_ICON] = value
    }

    companion object {
        private const val KEY_NAME = "name"
        private const val KEY_COLOR = "color"
        private const val KEY_ICON = "icon"
    }
}
