package org.tasks.compose.settings

import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.tasks.R
import org.tasks.extensions.Context.findActivity
import org.tasks.themes.colorOn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSettingsScaffold(
    title: String,
    theme: Color,
    promptDiscard: Boolean,
    showProgress: Boolean,
    dismissDiscardPrompt: () -> Unit,
    save: () -> Unit,
    discard: () -> Unit,
    actions: @Composable () -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            Column {
                val context = LocalContext.current
                val contentColor = colorOn(theme)
                LaunchedEffect(theme, contentColor, context) {
                    val systemBarStyle = if (contentColor == Color.White) {
                        SystemBarStyle.dark(0)
                    } else {
                        SystemBarStyle.light(0, 0)
                    }
                    (context.findActivity() as? AppCompatActivity)?.enableEdgeToEdge(
                        statusBarStyle = systemBarStyle,
                        navigationBarStyle = systemBarStyle
                    )
                }
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = theme,
                        navigationIconContentColor = contentColor,
                        titleContentColor = contentColor,
                        actionIconContentColor = contentColor,
                    ),
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = save,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = stringResource(R.string.save),
                            )
                        }
                    },
                    actions = {
                        actions()
                    }
                )
                ProgressBar(showProgress)
            }
        },
        floatingActionButton = { fab() },
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState()),
        ) {
            content()
        }
        PromptAction(
            showDialog = promptDiscard,
            title = stringResource(id = R.string.discard_changes),
            onAction = { discard() },
            onCancel = { dismissDiscardPrompt() },
        )
    }
}
