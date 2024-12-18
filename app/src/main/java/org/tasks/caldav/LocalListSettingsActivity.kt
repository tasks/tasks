package org.tasks.caldav

import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.tasks.compose.DeleteButton
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.themes.TasksTheme

@AndroidEntryPoint
class LocalListSettingsActivity : BaseCaldavCalendarSettingsActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val canDelete = runBlocking { caldavDao.getCalendarsByAccount(CaldavDao.LOCAL).size > 1 }

        setContent {
            TasksTheme {
                BaseCaldavSettingsContent (
                    optionButton = { if (!isNew && canDelete) DeleteButton { promptDelete() } }
                )
            }
        }
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) =
            createSuccessful(null)

    override suspend fun updateNameAndColor(
        account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) =
            updateCalendar()

    override suspend fun deleteCalendar(caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar) =
            onDeleted(true)
}