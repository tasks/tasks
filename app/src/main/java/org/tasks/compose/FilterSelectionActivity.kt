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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.pickers.SearchableFilterPicker
import org.tasks.dialogs.FilterPickerHiltViewModel
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class FilterSelectionActivity : AppCompatActivity() {

    @Inject lateinit var theme: Theme

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        val selected = IntentCompat.getParcelableExtra(intent, EXTRA_FILTER, Filter::class.java)
        setContent {
            TasksTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                val viewModel: FilterPickerHiltViewModel = hiltViewModel()
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
                    val isDark = isSystemInDarkTheme()
                    val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()
                    SearchableFilterPicker(
                        filters = if (searching) state.searchResults else state.filters,
                        query = state.query,
                        onQueryChange = { viewModel.onQueryChange(it) },
                        getIcon = { viewModel.getIcon(it) },
                        getColor = { filter ->
                            viewModel.getColor(filter.tint, isDark) ?: onSurface
                        },
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
                                        viewModel.updateWidget(widgetId, filter)
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

        fun Fragment.registerForListPickerResult(callback: (CaldavFilter) -> Unit): ActivityResultLauncher<Intent> {
            return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                it.data?.let { intent ->
                    IntentCompat
                        .getParcelableExtra(intent, EXTRA_FILTER, CaldavFilter::class.java)
                        ?.let(callback)
                }
            }
        }

        fun Fragment.registerForFilterPickerResult(callback: (Filter) -> Unit): ActivityResultLauncher<Intent> {
            return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                it.data?.let { intent ->
                    IntentCompat
                        .getParcelableExtra(intent, EXTRA_FILTER, Filter::class.java)
                        ?.let(callback)
                }
            }
        }

        fun ComponentActivity.registerForFilterPickerResult(callback: (Filter) -> Unit): ActivityResultLauncher<Intent> {
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
