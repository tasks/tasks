package org.tasks.compose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.compose.pickers.IconPicker
import org.tasks.compose.pickers.IconPickerViewModel
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class IconPickerActivity : AppCompatActivity() {
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var theme: Theme

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TasksTheme(theme = theme.themeBase.index) {
                var hasPro by remember { mutableStateOf(false) }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    hasPro = inventory.hasPro
                }
                val viewModel: IconPickerViewModel = viewModel()
                val searchResults = viewModel.searchResults.collectAsStateWithLifecycle().value
                val state = viewModel.viewState.collectAsStateWithLifecycle().value
                BasicAlertDialog(
                    onDismissRequest = { finish() },
                    modifier = Modifier.padding(vertical = 32.dp)
                ) {
                    IconPicker(
                        icons = state.icons,
                        query = state.query,
                        searchResults = searchResults,
                        collapsed = state.collapsed,
                        onQueryChange = { viewModel.onQueryChange(it) },
                        onSelected = {
                            setResult(RESULT_OK, Intent().putExtra(EXTRA_SELECTED, it.name))
                            finish()
                        },
                        toggleCollapsed = { category, collapsed ->
                            viewModel.setCollapsed(category, collapsed)
                        },
                        hasPro = hasPro,
                        subscribe = {
                            startActivity(Intent(this, PurchaseActivity::class.java))
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_SELECTED = "extra_selected"

        fun ComponentActivity.registerForIconPickerResult(callback: (String) -> Unit): ActivityResultLauncher<Intent> {
            return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                it.data?.getStringExtra(EXTRA_SELECTED)?.let(callback)
            }
        }

        fun ActivityResultLauncher<Intent>.launchIconPicker(
            context: Context,
            selected: String? = null,
        ) {
            launch(
                Intent(context, IconPickerActivity::class.java)
                    .putExtra(EXTRA_SELECTED, selected)
            )
        }
    }
}