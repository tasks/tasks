package org.tasks.caldav

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tasks.R
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity.Companion.EXTRA_CALDAV_ACCOUNT
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity.Companion.EXTRA_CALDAV_CALENDAR
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.preferences.Preferences
import org.tasks.preferences.TasksPreferences
import org.tasks.service.TaskDeleter
import org.tasks.viewmodel.LocalListSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class LocalListSettingsHiltViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val preferences: Preferences,
    caldavDao: CaldavDao,
    taskDeleter: TaskDeleter,
    reporting: Reporting,
    purchaseState: PurchaseState,
    tasksPreferences: TasksPreferences,
    @ApplicationContext context: Context,
) : LocalListSettingsViewModel(
    caldavDao = caldavDao,
    taskDeleter = taskDeleter,
    reporting = reporting,
    tasksPreferences = tasksPreferences,
    purchaseState = purchaseState,
    isDark = context.resources.getBoolean(R.bool.is_dark),
    account = savedStateHandle.get<CaldavAccount>(EXTRA_CALDAV_ACCOUNT)!!,
    calendar = savedStateHandle[EXTRA_CALDAV_CALENDAR]
        ?: CaldavCalendar(uuid = org.tasks.data.UUIDHelper.newUUID()),
    hasColorWheel = true,
) {
    private val _showBannerAndroid = MutableStateFlow(
        !preferences.getBoolean(R.string.p_local_list_banner_dismissed, false)
    )
    override val showBanner: StateFlow<Boolean> = _showBannerAndroid.asStateFlow()

    override fun dismissBanner() {
        _showBannerAndroid.value = false
        preferences.setBoolean(R.string.p_local_list_banner_dismissed, true)
    }

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
