package org.tasks.sync.todoist

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity.Companion.EXTRA_CALDAV_ACCOUNT
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity.Companion.EXTRA_CALDAV_CALENDAR
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.viewmodel.ListSettingsDeps
import org.tasks.viewmodel.ListSettingsTarget
import org.tasks.viewmodel.TodoistListSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class TodoistListSettingsHiltViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    deps: ListSettingsDeps,
    clientProvider: TodoistClientProvider,
    @ApplicationContext context: Context,
) : TodoistListSettingsViewModel(
    deps = deps,
    target = ListSettingsTarget(
        account = savedStateHandle.get<CaldavAccount>(EXTRA_CALDAV_ACCOUNT)!!,
        calendar = savedStateHandle[EXTRA_CALDAV_CALENDAR]
            ?: CaldavCalendar(uuid = org.tasks.data.UUIDHelper.newUUID()),
        isDark = context.resources.getBoolean(R.bool.is_dark),
        hasColorWheel = true,
    ),
    clientProvider = clientProvider,
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
