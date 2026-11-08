package org.tasks.compose.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.SymbolIcon
import org.tasks.themes.TasksIcons
import org.tasks.viewmodel.DrawerCustomizationItem
import org.tasks.viewmodel.DrawerCustomizationViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.customize_drawer
import tasks.kmp.generated.resources.reset_order

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerCustomizationDetail(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<DrawerCustomizationViewModel>()
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.customize_drawer)) },
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
        DrawerCustomizationScreen(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

/**
 * The compose counterpart of the native "Customize drawer" screen: drag
 * reorder the drawer's own sections (custom filters, tags, places, and the
 * lists under each account).
 *
 * The sh.calvin reorderable library owns the gesture and the live
 * reordering; it calls [DrawerCustomizationViewModel.onMove] once per row
 * crossed (state-only) and [DrawerCustomizationViewModel.onDragStopped] on
 * release (commits the section's order column). The library captures its
 * callbacks once, so the move body is handed through a holder that is
 * re-assigned on every recomposition - the same pattern the task-row drag
 * uses (see TaskList.kt).
 */
@Composable
fun DrawerCustomizationScreen(
    viewModel: DrawerCustomizationViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val moveRef = remember { mutableStateOf<(Any, Any) -> Unit>({ _, _ -> }) }
    moveRef.value = { fromKey, toKey -> viewModel.onMove(fromKey, toKey) }
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        moveRef.value(from.key, to.key)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (!state.loaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {
            state.sections.forEach { section ->
                item(key = "header-${section.id}") {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    )
                }
                items(section.items, key = { it.key }) { list ->
                    ReorderableItem(reorderableState, list.key) { isDragging ->
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            label = "drawer-customization-drag",
                        )
                        Surface(
                            shadowElevation = elevation,
                            color = if (isDragging) {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            } else {
                                Color.Transparent
                            },
                        ) {
                            DrawerCustomizationRow(
                                item = list,
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

@Composable
private fun DrawerCustomizationRow(
    item: DrawerCustomizationItem,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = item.count?.takeIf { it > 0 }?.let { count ->
            { Text(count.toString()) }
        },
        leadingContent = item.icon?.takeIf { it.isNotBlank() }?.let { icon ->
            {
                SymbolIcon(
                    name = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = modifier,
    )
}
