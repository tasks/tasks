package org.tasks.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel
import org.tasks.compose.settings.GoogleTaskListSettingsScreen
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

                GoogleTaskListSettingsScreen(
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
                        if (viewModel.state.value.hasPro || it.isFree) {
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
                        if (viewModel.state.value.hasPro) {
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
                    BackHandler {
                        showColorWheel = false
                        viewModel.openColorPicker()
                    }
                    val context = LocalContext.current
                    var selected by remember { mutableIntStateOf(0) }
                    val initialColor = viewModel.state.value.color
                    DisposableEffect(Unit) {
                        val dialog = ColorPickerDialogBuilder
                            .with(context)
                            .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                            .density(7)
                            .setOnColorChangedListener { which -> selected = which }
                            .setOnColorSelectedListener { which -> selected = which }
                            .lightnessSliderOnly()
                            .setPositiveButton(R.string.ok) { _, _, _ ->
                                viewModel.setColor(selected)
                            }
                            .setNegativeButton(R.string.cancel) { _, _ ->
                                viewModel.openColorPicker()
                            }
                            .apply {
                                if (initialColor != 0) {
                                    initialColor(initialColor)
                                }
                            }
                            .build()
                            .apply {
                                setOnDismissListener { showColorWheel = false }
                            }
                        dialog.show()
                        onDispose { dialog.dismiss() }
                    }
                }
            }
        }
    }
}
