package org.tasks.compose

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.LocalBroadcastManager
import org.tasks.compose.pickers.SearchableFilterPicker
import org.tasks.dialogs.FilterPickerViewModel
import org.tasks.filters.Filter
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.widget.WidgetPreferences
import javax.inject.Inject

@AndroidEntryPoint
class FilterSelectionActivity : AppCompatActivity() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        val selected = IntentCompat.getParcelableExtra(intent, EXTRA_FILTER, Filter::class.java)
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                val viewModel: FilterPickerViewModel = viewModel()
                val state = viewModel.viewState.collectAsStateWithLifecycle().value
                BasicAlertDialog(
                    onDismissRequest = { finish() },
                    modifier = Modifier.padding(vertical = 32.dp)
                ) {
                    val searching by remember(state.query) {
                        derivedStateOf {
                            state.query.isNotBlank()
                        }
                    }
                    BackHandler {
                        if (searching) {
                            viewModel.onQueryChange("")
                        } else {
                            finish()
                        }
                    }
                    SearchableFilterPicker(
                        filters = if (searching) state.searchResults else state.filters,
                        query = state.query,
                        onQueryChange = { viewModel.onQueryChange(it) },
                        getIcon = { ImageVector.vectorResource(id = viewModel.getIcon(it)) },
                        getColor = { viewModel.getColor(it) },
                        selected = selected,
                        onClick = { filter ->
                            when (filter) {
                                is NavigationDrawerSubheader -> {
                                    viewModel.onClick(filter)
                                }

                                is Filter -> {
                                    val data = Bundle()
                                    data.putParcelable(EXTRA_FILTER, filter)
                                    if (widgetId != -1) {
                                        WidgetPreferences(this, preferences, widgetId)
                                            .setFilter(
                                                defaultFilterProvider.getFilterPreferenceValue(
                                                    filter
                                                )
                                            )
                                        localBroadcastManager.reconfigureWidget(widgetId)
                                    }
                                    setResult(RESULT_OK, Intent().putExtras(data))
                                    finish()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_FILTER = "extra_filter"
        const val EXTRA_LISTS_ONLY = "extra_lists_only"

        fun Fragment.registerForListPickerResult(callback: (Filter) -> Unit): ActivityResultLauncher<Intent> {
            return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                it.data?.let { intent ->
                    IntentCompat
                        .getParcelableExtra(intent, EXTRA_FILTER, Filter::class.java)
                        ?.let(callback)
                }
            }
        }

        fun ComponentActivity.registerForListPickerResult(callback: (Filter) -> Unit): ActivityResultLauncher<Intent> {
            return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                it.data?.let { intent ->
                    IntentCompat
                        .getParcelableExtra(intent, EXTRA_FILTER, Filter::class.java)
                        ?.let(callback)
                }
            }
        }

        fun ActivityResultLauncher<Intent>.launch(
            context: Context,
            selectedFilter: Filter? = null,
            listsOnly: Boolean = false,
        ) {
            launch(
                Intent(context, FilterSelectionActivity::class.java)
                    .putExtra(EXTRA_FILTER, selectedFilter)
                    .putExtra(EXTRA_LISTS_ONLY, listsOnly)
            )
        }
    }
}
