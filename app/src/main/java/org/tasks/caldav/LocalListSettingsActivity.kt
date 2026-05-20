package org.tasks.caldav

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
import org.jetbrains.compose.resources.stringResource
import org.tasks.R
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel
import org.tasks.compose.ColorWheelDialog
import org.tasks.compose.components.AnimatedBanner
import org.tasks.compose.settings.ListSettingsScreen
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_account
import tasks.kmp.generated.resources.dismiss
import tasks.kmp.generated.resources.local_list_description
import tasks.kmp.generated.resources.local_list_title
import javax.inject.Inject

@AndroidEntryPoint
class LocalListSettingsActivity : AppCompatActivity() {

    @Inject lateinit var preferences: Preferences

    private val viewModel: LocalListSettingsHiltViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TasksTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                var showColorWheel by rememberSaveable { mutableStateOf(false) }
                var bannerVisible by rememberSaveable {
                    mutableStateOf(
                        state.isNew && !preferences.getBoolean(
                            R.string.p_local_list_banner_dismissed, false
                        )
                    )
                }

                ListSettingsScreen(
                    viewModel = viewModel,
                    onSave = {
                        viewModel.save(
                            onDismiss = { finish() },
                            onComplete = { account, calendar ->
                                preferences.setBoolean(
                                    R.string.p_local_list_banner_dismissed, true
                                )
                                setResult(
                                    Activity.RESULT_OK,
                                    Intent(TaskListFragment.ACTION_RELOAD).putExtra(
                                        MainActivity.OPEN_FILTER,
                                        CaldavFilter(
                                            calendar = calendar,
                                            account = account,
                                        ),
                                    )
                                )
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
                                    .putExtra(
                                        PurchaseActivityViewModel.EXTRA_SOURCE,
                                        "list_colors"
                                    )
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
                                    .putExtra(
                                        PurchaseActivityViewModel.EXTRA_SOURCE,
                                        "list_colors"
                                    )
                            )
                        }
                    },
                    onSubscribe = {
                        startActivity(
                            Intent(this, PurchaseActivity::class.java)
                                .putExtra(PurchaseActivityViewModel.EXTRA_SOURCE, "icons")
                        )
                    },
                    headerContent = {
                        AnimatedBanner(
                            visible = bannerVisible,
                            title = stringResource(Res.string.local_list_title),
                            body = stringResource(Res.string.local_list_description),
                            dismissText = stringResource(Res.string.dismiss),
                            onDismiss = {
                                bannerVisible = false
                                preferences.setBoolean(
                                    R.string.p_local_list_banner_dismissed, true
                                )
                            },
                            action = stringResource(Res.string.add_account),
                            onAction = {
                                startActivity(
                                    Intent(
                                        this@LocalListSettingsActivity,
                                        MainActivity::class.java
                                    ).putExtra(MainActivity.OPEN_ADD_ACCOUNT, true)
                                )
                                finish()
                            },
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
