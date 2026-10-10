package org.tasks.compose.settings

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.viewmodel.NavigationDrawerViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.customize_drawer
import tasks.kmp.generated.resources.navigation_drawer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawerDetail(
    onNavigateBack: () -> Unit,
    onItemClick: (org.tasks.filters.FilterListItem) -> Unit = {},
    onCreateNew: (org.tasks.filters.NavigationDrawerSubheader) -> Unit = {},
) {
    val viewModel = koinViewModel<NavigationDrawerViewModel>()
    val customizationVM = koinViewModel<NavigationDrawerCustomizationViewModel>()
    var showCustomization by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showCustomization) stringResource(Res.string.customize_drawer) else stringResource(Res.string.navigation_drawer)) },
                navigationIcon = {
                    IconButton(onClick = { if (showCustomization) { showCustomization = false } else { onNavigateBack() } }) {
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
            if (showCustomization) {
                NavigationDrawerCustomization(
                    items = customizationVM.items.collectAsState(emptyList()).value,
                    collapsedSections = customizationVM.collapsedSections.collectAsState(emptySet()).value,
                    callbacks = NavigationDrawerCustomizationCallbacks(
                        onToggleCollapse = { customizationVM.toggleSectionCollapse(it) },
                        onCreateNew = onCreateNew,
                        onReorder = { from, to -> customizationVM.swapItems(from, to) },
                        onItemClick = onItemClick,
                    ),
                    onBack = { showCustomization = false },
                )
            } else {
                NavigationDrawerContent(
                    viewModel = viewModel,
                    onCustomizeDrawer = { showCustomization = true },
                )
            }
        }
    }
}
