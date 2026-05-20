package org.tasks.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel
import org.tasks.compose.ColorWheelDialog
import org.tasks.compose.settings.ListSettingsScreen
import org.tasks.filters.CaldavFilter
import org.tasks.themes.TasksTheme

@AndroidEntryPoint
class GoogleTaskListSettingsActivity : AppCompatActivity() {

    private val viewModel: GoogleTaskListSettingsHiltViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TasksTheme {
                var showColorWheel by rememberSaveable { mutableStateOf(false) }

                val state by viewModel.state.collectAsStateWithLifecycle()

                ListSettingsScreen(
                    viewModel = viewModel,
                    onSave = {
                        viewModel.save(
                            onDismiss = { finish() },
                            onComplete = { calendar ->
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
                            },
                        )
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
                        if (state.hasPro || it.isFree) {
                            viewModel.selectColor(it.originalColor)
                        } else {
                            viewModel.closeColorPicker()
                            startActivity(
                                Intent(this, PurchaseActivity::class.java)
                                    .putExtra(PurchaseActivityViewModel.EXTRA_SOURCE, "list_colors")
                            )
                        }
                    },
                    onColorWheelSelected = {
                        viewModel.closeColorPicker()
                        if (state.hasPro) {
                            showColorWheel = true
                        } else {
                            startActivity(
                                Intent(this, PurchaseActivity::class.java)
                                    .putExtra(PurchaseActivityViewModel.EXTRA_SOURCE, "list_colors")
                            )
                        }
                    },
                    onSubscribe = {
                        startActivity(
                            Intent(this, PurchaseActivity::class.java)
                                .putExtra(PurchaseActivityViewModel.EXTRA_SOURCE, "icons")
                        )
                    },
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
