/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.tasks.activities

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
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoroo.astrid.activity.TaskListFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.analytics.Firebase
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel
import org.tasks.compose.ColorWheelDialog
import org.tasks.compose.settings.TagSettingsScreen
import org.tasks.compose.settings.canPinShortcut
import org.tasks.compose.settings.canPinWidget
import org.tasks.compose.settings.createShortcut
import org.tasks.compose.settings.createWidget
import org.tasks.compose.settings.setReloadResult
import org.tasks.filters.TagFilter
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.themes.TasksTheme
import javax.inject.Inject

@AndroidEntryPoint
class TagSettingsActivity : AppCompatActivity() {

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var firebase: Firebase

    private val viewModel: TagSettingsHiltViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TasksTheme {
                val state by viewModel.viewState.collectAsStateWithLifecycle()
                var showColorWheel by rememberSaveable { mutableStateOf(false) }
                val primaryColor = MaterialTheme.colorScheme.primary
                val canAddShortcut = remember { canPinShortcut() }
                val canAddWidget = remember { canPinWidget() }

                TagSettingsScreen(
                    viewModel = viewModel,
                    onSave = {
                        viewModel.save(
                            onDismiss = { finish() },
                            onComplete = { tag ->
                                setReloadResult(TagFilter(tag))
                                finish()
                            },
                        )
                    },
                    onDelete = {
                        viewModel.delete { uuid ->
                            setResult(
                                Activity.RESULT_OK,
                                Intent(TaskListFragment.ACTION_DELETED).putExtra(EXTRA_TAG_UUID, uuid),
                            )
                            finish()
                        }
                    },
                    onNavigateBack = { finish() },
                    onSubscribe = { source ->
                        startActivity(
                            Intent(this, PurchaseActivity::class.java)
                                .putExtra(PurchaseActivityViewModel.EXTRA_SOURCE, source)
                        )
                    },
                    onColorWheelSelected = { showColorWheel = true },
                    onAddShortcut = if (canAddShortcut) {
                        {
                            viewModel.persist { tag ->
                                createShortcut(
                                    filter = TagFilter(tag),
                                    title = tag.name ?: "",
                                    icon = tag.icon,
                                    color = (tag.color ?: 0).takeIf { it != 0 }?.let { Color(it) } ?: primaryColor,
                                    defaultFilterProvider = defaultFilterProvider,
                                    firebase = firebase,
                                )
                                setReloadResult(TagFilter(tag))
                                finish()
                            }
                        }
                    } else null,
                    onAddWidget = if (canAddWidget) {
                        {
                            viewModel.persist { tag ->
                                createWidget(
                                    filter = TagFilter(tag),
                                    color = tag.color ?: 0,
                                    defaultFilterProvider = defaultFilterProvider,
                                    firebase = firebase,
                                )
                                setReloadResult(TagFilter(tag))
                                finish()
                            }
                        }
                    } else null,
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

    companion object {
        const val EXTRA_TAG_DATA = "tagData" // $NON-NLS-1$
        private const val EXTRA_TAG_UUID = "uuid" // $NON-NLS-1$
    }
}
