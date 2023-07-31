package org.tasks.repeats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.pickers.CustomRecurrence

@AndroidEntryPoint
class CustomRecurrenceActivity : FragmentActivity() {
    val viewModel: CustomRecurrenceViewModel by viewModels()

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setContent {
            MdcTheme {
                CustomRecurrence(
                    state = viewModel.state.collectAsStateLifecycleAware().value,
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
