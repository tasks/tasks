package org.tasks.compose.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.compose.components.SymbolIcon
import org.tasks.themes.TasksIcons
import org.tasks.viewmodel.ListOrderViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.list_order
import tasks.kmp.generated.resources.list_order_empty
import tasks.kmp.generated.resources.reset_order

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListOrderDetail(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<ListOrderViewModel>()
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.list_order)) },
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
                        onClick = { viewModel.resetOrder() },
                        enabled = state.hasManualOrder,
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
        ListOrderScreen(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

/**
 * The reorder screen: one section per account, each list row draggable.
 *
 * The sh.calvin reorderable library owns the gesture and the live reordering;
 * it calls [ListOrderViewModel.onMove] once per row crossed (state-only) and
 * [ListOrderViewModel.onDragStopped] on release (commits `cdl_order`). The
 * library captures its callbacks once, so the move body is handed through a
 * holder that is re-assigned on every recomposition - the same pattern the
 * task-row drag uses (see TaskList.kt). The view model reference is stable,
 * so the stop callback can capture it directly.
 */
@Composable
fun ListOrderScreen(
    viewModel: ListOrderViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val moveRef = remember { mutableStateOf<(Any, Any) -> Unit>({ _, _ -> }) }
    moveRef.value = { fromKey, toKey -> viewModel.onMove(fromKey, toKey) }
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        // Header keys are Strings, list keys are Long calendarIds; the view
        // model no-ops anything that is not a Long->Long same-section move.
        moveRef.value(from.key, to.key)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (!state.loaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            return
        }
        if (state.sections.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    text = stringResource(Res.string.list_order_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {
            state.sections.forEach { section ->
                item(key = "header-${section.accountUuid}") {
                    Text(
                        text = section.accountName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    )
                }
                items(section.lists, key = { it.calendarId }) { list ->
                    ReorderableItem(reorderableState, list.calendarId) { isDragging ->
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            label = "list-order-drag",
                        )
                        Surface(
                            shadowElevation = elevation,
                            color = if (isDragging) {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            } else {
                                Color.Transparent
                            },
                        ) {
                            ListItem(
                                headlineContent = { Text(list.name) },
                                modifier = Modifier.draggableHandle(
                                    onDragStopped = { viewModel.onDragStopped() },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
