package org.tasks.etebase

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.analytics.Firebase
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel
import org.tasks.compose.ColorWheelDialog
import org.tasks.compose.settings.ListSettingsScreen
import org.tasks.compose.settings.addShortcutCallback
import org.tasks.compose.settings.addWidgetCallback
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.themes.TasksTheme
import javax.inject.Inject

@AndroidEntryPoint
class EtebaseCalendarSettingsActivity : AppCompatActivity() {

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var firebase: Firebase

    private val viewModel: EtebaseCalendarSettingsHiltViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TasksTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                var showColorWheel by rememberSaveable { mutableStateOf(false) }
                val primaryColor = MaterialTheme.colorScheme.primary

                ListSettingsScreen(
                    viewModel = viewModel,
                    onSave = {
                        viewModel.save { calendar ->
                            val account = viewModel.state.value.account
                            if (account != null) {
                                setResult(
                                    Activity.RESULT_OK,
                                    Intent(TaskListFragment.ACTION_RELOAD).putExtra(
                                        MainActivity.OPEN_FILTER,
                                        CaldavFilter(calendar = calendar, account = account),
                                    )
                                )
                            }
                            finish()
                        }
                    },
                    onDelete = {
                        viewModel.delete {
                            setResult(
                                Activity.RESULT_OK,
                                Intent(TaskListFragment.ACTION_DELETED),
                            )
                            finish()
                        }
                    },
                    onNavigateBack = { finish() },
                    onSelectColor = {
                        viewModel.selectColor(it?.originalColor ?: 0)
                    },
                    onColorWheelSelected = {
                        viewModel.closeColorPicker()
                        showColorWheel = true
                    },
                    onSubscribe = { source ->
                        startActivity(
                            Intent(this, PurchaseActivity::class.java)
                                .putExtra(PurchaseActivityViewModel.EXTRA_SOURCE, source)
                        )
                    },
                    onAddShortcut = remember { addShortcutCallback(viewModel.state::value, primaryColor, defaultFilterProvider, firebase) },
                    onAddWidget = remember { addWidgetCallback(viewModel.state::value, defaultFilterProvider, firebase) },
                )

                if (showColorWheel) {
                    ColorWheelDialog(
                        initialColor = state.color,
                        onColorSelected = viewModel::setColor,
                        onCancel = {
                            showColorWheel = false
                            viewModel.openColorPicker()
                        },
                        onDismiss = { showColorWheel = false },
                    )
                }
            }
        }
    }
}
