package org.tasks.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.viewmodel.NavigationDrawerViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.navigation_drawer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawerDetail(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<NavigationDrawerViewModel>()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.navigation_drawer)) },
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
            NavigationDrawerContent(
                viewModel = viewModel,
            )
        }
    }
}
