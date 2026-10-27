package org.tasks.compose.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.tasks.compose.components.SymbolIcon
import org.tasks.compose.edit.EditScreenSectionItem
import org.tasks.themes.TasksIcons
import org.tasks.viewmodel.EditScreenCustomizationViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.EPr_customize_edit_screen
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.reset_order

/**
 * The desktop "Customize edit screen" screen: reorder the blocks on the task
 * edit screen and hide the ones the user never uses (the native app reaches
 * the equivalent through its legacy BeastMode screen).
 *
 * Each row's drag handle comes from the sh.calvin reorderable library (same
 * pattern as the task list drag and the drawer customisation screen): the
 * library calls [EditScreenCustomizationViewModel.onMove] once per row
 * crossed, and the move body is handed through a holder that is re-assigned
 * on every recomposition because the library captures its callbacks once.
 *
 * The handle is the leading icon rather than the whole row so the
 * show/hide switch keeps ordinary click handling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreenCustomizationDetail(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinInject<EditScreenCustomizationViewModel>()
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.EPr_customize_edit_screen)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            name = TasksIcons.ARROW_BACK,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.reset() },
                        enabled = state.loaded && viewModel.isCustomized(),
                    ) {
                        SymbolIcon(
                            name = TasksIcons.REFRESH,
                            contentDescription = stringResource(Res.string.reset_order),
                        )
                    }
                },
            )
        },
    ) { padding ->
        EditScreenCustomizationScreen(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
fun EditScreenCustomizationScreen(
    viewModel: EditScreenCustomizationViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val moveRef = remember { mutableStateOf<(String, String) -> Unit>({ _, _ -> }) }
    moveRef.value = { fromId, toId -> viewModel.onMove(fromId, toId) }
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        moveRef.value(from.key.toString(), to.key.toString())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (!state.loaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {
            items(state.sections, key = { it.id }) { section ->
                ReorderableItem(reorderableState, section.id) { isDragging ->
                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 8.dp else 0.dp,
                        label = "edit-screen-customization-drag",
                    )
                    Surface(
                        shadowElevation = elevation,
                        color = if (isDragging) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            Color.Transparent
                        },
                    ) {
                        EditScreenCustomizationRow(
                            item = section,
                            onToggle = { enabled -> viewModel.setEnabled(section.id, enabled) },
                            modifier = Modifier.draggableHandle(onDragStopped = {}),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditScreenCustomizationRow(
    item: EditScreenSectionItem,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(stringResource(item.section.titleRes)) },
        leadingContent = {
            Box(
                modifier = modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(
                    name = TasksIcons.DRAG_INDICATOR,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Switch(
                checked = item.enabled,
                onCheckedChange = onToggle,
            )
        },
    )
}
