package org.tasks.compose.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res

@Composable
fun IconPickerDialog(
    selectedIcon: String?,
    onIconSelected: (String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val viewModel = remember { IconPickerViewModel() }
    val viewState by viewModel.viewState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Icon Picker") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                org.tasks.compose.pickers.IconPicker(
                    icons = viewState.icons,
                    searchResults = searchResults,
                    collapsed = viewState.collapsed,
                    query = viewState.query,
                    onQueryChange = { viewModel.onQueryChange(it) },
                    onSelected = { selectedIcon ->
                        onIconSelected(selectedIcon.name)
                    },
                    toggleCollapsed = { category, collapsed ->
                        viewModel.setCollapsed(category, collapsed)
                    },
                    hasPro = true,
                    subscribe = { },
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}
