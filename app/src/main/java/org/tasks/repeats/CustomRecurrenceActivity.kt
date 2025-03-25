package org.tasks.repeats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.pickers.CustomRecurrence
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class CustomRecurrenceActivity : FragmentActivity() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var preferences: Preferences

    val viewModel: CustomRecurrenceViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setContent {
            TasksTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                CustomRecurrence(
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    save = {
                        setResult(RESULT_OK, Intent().putExtra(EXTRA_RRULE, viewModel.getRecur()))
                        finish()
                    },
                    discard = { finish() },
                    setSelectedFrequency = { viewModel.setFrequency(it) },
                    setEndDate = { viewModel.setEndDate(it) },
                    setSelectedEndType = { viewModel.setEndType(it) },
                    setInterval = { viewModel.setInterval(it) },
                    setOccurrences = { viewModel.setOccurrences(it) },
                    toggleDay = { viewModel.toggleDay(it) },
                    setMonthSelection = { viewModel.setMonthSelection(it) },
                    calendarDisplayMode = remember { preferences.calendarDisplayMode },
                    setDisplayMode = { preferences.calendarDisplayMode = it }
                )
            }
        }
    }

    companion object {
        const val EXTRA_RRULE = "extra_rrule"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_ACCOUNT_TYPE = "extra_account_type"

        @JvmStatic
        fun newIntent(context: Context, rrule: String?, dueDate: Long, accountType: Int) =
            Intent(context, CustomRecurrenceActivity::class.java)
                .putExtra(EXTRA_DATE, dueDate)
                .putExtra(EXTRA_RRULE, rrule)
                .putExtra(EXTRA_ACCOUNT_TYPE, accountType)
    }
}
