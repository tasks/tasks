package org.tasks.preferences.fragments

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.PrincipalDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.service.TaskDeleter
import org.tasks.sync.SyncAdapters
import org.tasks.viewmodel.CaldavCalendarSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class CaldavCalendarSettingsHiltViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    caldavDao: CaldavDao,
    caldavClientProvider: CaldavClientProvider,
    principalDao: PrincipalDao,
    taskDeleter: TaskDeleter,
    syncAdapters: SyncAdapters,
    reporting: Reporting,
    purchaseState: PurchaseState,
    @ApplicationContext context: Context,
) : CaldavCalendarSettingsViewModel(
    caldavDao = caldavDao,
    caldavClientProvider = caldavClientProvider,
    principalDao = principalDao,
    taskDeleter = taskDeleter,
    syncAdapters = syncAdapters,
    reporting = reporting,
    purchaseState = purchaseState,
    isDark = context.resources.getBoolean(R.bool.is_dark),
    hasColorWheel = true,
) {
    init {
        val account: CaldavAccount =
            savedStateHandle[BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_ACCOUNT]!!
        val calendar: CaldavCalendar? =
            savedStateHandle[BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_CALENDAR]
        setCalendar(account, calendar)
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
