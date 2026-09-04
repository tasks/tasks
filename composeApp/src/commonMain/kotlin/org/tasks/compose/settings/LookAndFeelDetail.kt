package org.tasks.compose.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.tasks.billing.PurchaseState
import org.tasks.broadcast.ComposeRefreshBroadcaster
import org.tasks.compose.edit.ListPickerDialog
import org.tasks.extensions.restartApplication
import org.tasks.filters.Filter
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.viewmodel.FilterPickerViewModel
import org.tasks.viewmodel.LookAndFeelViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.preferences_look_and_feel
import tasks.kmp.generated.resources.url_translations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookAndFeelDetail(
    onNavigateBack: () -> Unit,
    onSubscribe: () -> Unit = {},
) {
    val viewModel = koinViewModel<LookAndFeelViewModel>()
    val uriHandler = LocalUriHandler.current
    val translationsUrl = stringResource(Res.string.url_translations)
    val refreshBroadcaster = koinInject<ComposeRefreshBroadcaster>()
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var showFilterPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        refreshBroadcaster.refreshes.collect { viewModel.refreshState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.preferences_look_and_feel)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            LookAndFeelContent(
                viewModel = viewModel,
                onColor = { showColorPicker = true },
                onDefaultFilter = { showFilterPicker = true },
                onTranslations = { uriHandler.openUri(translationsUrl) },
                onRestartApplication = { restartApplication() },
            )
        }
    }

    if (showColorPicker) {
        val isDark = isSystemInDarkTheme()
        val purchaseState = koinInject<PurchaseState>()
        val colors = remember(isDark) { buildPickerColors(isDark) }
        ColorPickerDialog(
            hasPro = purchaseState.purchasedThemes(),
            colors = colors,
            onDismiss = { showColorPicker = false },
            onColorSelected = { viewModel.setThemeColor(it.originalColor) },
            onSubscribe = {
                showColorPicker = false
                onSubscribe()
            },
            showColorWheel = false,
        )
    }

    if (showFilterPicker && viewModel.loaded) {
        val isDark = isSystemInDarkTheme()
        val pickerViewModel = koinViewModel<FilterPickerViewModel>(
            key = "look_and_feel_filter_picker",
            parameters = { parametersOf(false) },
        )
        val pickerState by pickerViewModel.viewState.collectAsState()
        val searching = pickerState.query.isNotBlank()
        val onSurfaceArgb = MaterialTheme.colorScheme.onSurface.toArgb()
        val dismiss = {
            showFilterPicker = false
            pickerViewModel.onQueryChange("")
        }
        ListPickerDialog(
            filters = if (searching) pickerState.searchResults else pickerState.filters,
            query = pickerState.query,
            onQueryChange = pickerViewModel::onQueryChange,
            selected = viewModel.defaultFilter,
            onClick = { filter ->
                when (filter) {
                    is NavigationDrawerSubheader -> pickerViewModel.onClick(filter)
                    is Filter -> {
                        viewModel.setDefaultOpenFilter(filter)
                        dismiss()
                    }
                    else -> Unit
                }
            },
            getIcon = { pickerViewModel.getIcon(it) },
            getColor = { pickerViewModel.getColor(it.tint, isDark) ?: onSurfaceArgb },
            onDismiss = dismiss,
        )
    }
}
