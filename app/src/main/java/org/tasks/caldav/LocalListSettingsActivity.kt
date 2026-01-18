package org.tasks.caldav

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.todoroo.astrid.activity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.DeleteButton
import org.tasks.compose.components.AnimatedBanner
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import javax.inject.Inject

@AndroidEntryPoint
class LocalListSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    @Inject lateinit var preferences: Preferences

    private val showLocalListBanner: Boolean
        get() = isNew && !preferences.getBoolean(R.string.p_local_list_banner_dismissed, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TasksTheme {
                var bannerVisible by rememberSaveable { mutableStateOf(showLocalListBanner) }
                BaseCaldavSettingsContent (
                    optionButton = { if (!isNew) DeleteButton(caldavCalendar?.name ?: "") { delete() } },
                    headerContent = {
                        AnimatedBanner(
                            visible = bannerVisible,
                            title = stringResource(R.string.local_list_title),
                            body = stringResource(R.string.local_list_description),
                            dismissText = stringResource(R.string.dismiss),
                            onDismiss = {
                                bannerVisible = false
                                preferences.setBoolean(R.string.p_local_list_banner_dismissed, true)
                            },
                            action = stringResource(R.string.add_account),
                            onAction = {
                                startActivity(
                                    Intent(this@LocalListSettingsActivity, MainActivity::class.java)
                                        .putExtra(MainActivity.OPEN_ADD_ACCOUNT, true)
                                )
                                finish()
                            },
                        )
                    }
                )
            }
        }
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) {
        preferences.setBoolean(R.string.p_local_list_banner_dismissed, true)
        createSuccessful(null)
    }

    override suspend fun updateNameAndColor(
        account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) =
            updateCalendar()

    // TODO: prevent deleting the last list
    override suspend fun deleteCalendar(caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar) =
            onDeleted(true)
}
