package org.tasks.compose.settings

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.compose.filters.FilterSettingsScreen
import org.tasks.compose.filters.NewFilterTemplateDialog
import org.tasks.data.dao.FilterDao
import org.tasks.data.entity.Filter
import org.tasks.filters.FilterSettingsViewModel
import org.tasks.themes.isDarkTheme
import org.tasks.viewmodel.NavigationDrawerViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.custom_filters
import tasks.kmp.generated.resources.new_filter
import tasks.kmp.generated.resources.navigation_drawer

private data class FilterEditorSession(
    val filter: Filter?,
    val templateIndex: Int?,
    val key: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawerDetail(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<NavigationDrawerViewModel>()
    val refreshBroadcaster = koinInject<RefreshBroadcaster>()
    val isDark = isDarkTheme()

    var showTemplateDialog by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<FilterEditorSession?>(null) }
    var openTick by remember { mutableIntStateOf(0) }
    var listTick by remember { mutableIntStateOf(0) }

    val openNew = { index: Int ->
        openTick++
        showTemplateDialog = false
        editor = FilterEditorSession(
            filter = null,
            templateIndex = index,
            key = "filter-pane-new-$openTick",
        )
    }
    val openEdit = { filter: Filter ->
        openTick++
        editor = FilterEditorSession(
            filter = filter,
            templateIndex = null,
            key = "filter-pane-edit-${filter.id}-$openTick",
        )
    }
    val closeAndRefresh = {
        editor = null
        listTick++
        refreshBroadcaster.broadcastRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.navigation_drawer)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            name = TasksIcons.ARROW_BACK,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NavigationDrawerContent(
                viewModel = viewModel,
                filterSection = {
                    CustomFilterSection(
                        refreshKey = listTick,
                        onAdd = { showTemplateDialog = true },
                        onEdit = openEdit,
                    )
                },
            )

            if (showTemplateDialog) {
                NewFilterTemplateDialog(
                    onSelected = openNew,
                    onDismiss = { showTemplateDialog = false },
                )
            }

            editor?.let { session ->
                val filterViewModel = koinViewModel<FilterSettingsViewModel>(
                    key = session.key,
                    parameters = {
                        org.koin.core.parameter.parametersOf(
                            isDark,
                            session.filter,
                            session.templateIndex,
                        )
                    },
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    FilterSettingsScreen(
                        viewModel = filterViewModel,
                        onSave = { closeAndRefresh() },
                        onDelete = { closeAndRefresh() },
                        onNavigateBack = { editor = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomFilterSection(
    refreshKey: Int,
    onAdd: () -> Unit,
    onEdit: (Filter) -> Unit,
) {
    // Desktop-only custom-filters list, rendered into the shared navigation-drawer settings
    // screen via its optional [filterSection] slot. Android never passes one, so its
    // layout is unchanged.
    val filterDao = koinInject<FilterDao>()
    val filters by produceState(initialValue = emptyList(), key1 = refreshKey) {
        value = filterDao.getFilters()
    }

    SectionHeader(
        title = stringResource(Res.string.custom_filters),
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
    )
    Column(
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
    ) {
        val total = filters.size + 1
        SettingsItemCard(position = CardPosition.forIndex(0, total)) {
            PreferenceRow(
                title = stringResource(Res.string.new_filter),
                showChevron = true,
                onClick = onAdd,
            )
        }
        filters.forEachIndexed { index, filter ->
            SettingsItemCard(position = CardPosition.forIndex(index + 1, total)) {
                PreferenceRow(
                    title = filter.title ?: "",
                    icon = filter.icon,
                    showChevron = true,
                    onClick = { onEdit(filter) },
                )
            }
        }
    }
}
