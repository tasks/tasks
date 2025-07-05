package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.R
import org.tasks.sync.AddAccountDialog.Platform
import org.tasks.themes.TasksTheme

@Composable
fun AddAccountDialog(
    hasTasksAccount: Boolean,
    hasPro: Boolean,
    selected: (Platform) -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (!hasTasksAccount) {
            SyncAccount(
                title = R.string.tasks_org,
                cost = R.string.cost_more_money,
                description = R.string.tasks_org_description,
                icon = R.drawable.ic_round_icon,
                onClick = { selected(Platform.TASKS_ORG) }
            )
        }
        SyncAccount(
            title = R.string.gtasks_GPr_header,
            cost = if (hasPro) null else R.string.cost_free,
            description = R.string.google_tasks_selection_description,
            icon = R.drawable.ic_google,
            onClick = { selected(Platform.GOOGLE_TASKS) }
        )
        SyncAccount(
            title = R.string.todoist,
            cost = if (hasPro) null else R.string.cost_free,
            description = R.string.todoist_selection_description,
            icon = R.drawable.ic_todoist,
            onClick = { selected(Platform.TODOIST) }
        )
        SyncAccount(
            title = R.string.microsoft,
            cost = if (hasPro) null else R.string.cost_free,
            description = R.string.microsoft_selection_description,
            icon = R.drawable.ic_microsoft_tasks,
            onClick = { selected(Platform.MICROSOFT) }
        )
        SyncAccount(
            title = R.string.davx5,
            cost = if (hasPro) null else R.string.cost_money,
            description = R.string.davx5_selection_description,
            icon = R.drawable.ic_davx5_icon_green_bg,
            onClick = { selected(Platform.DAVX5) }
        )
        SyncAccount(
            title = R.string.caldav,
            cost = if (hasPro) null else R.string.cost_money,
            description = R.string.caldav_selection_description,
            icon = R.drawable.ic_webdav_logo,
            tint = MaterialTheme.colorScheme.onSurface.copy(
                alpha = .8f
            ),
            onClick = { selected(Platform.CALDAV) }
        )
        SyncAccount(
            title = R.string.etesync,
            cost = if (hasPro) null else R.string.cost_money,
            description = R.string.etesync_selection_description,
            icon = R.drawable.ic_etesync,
            onClick = { selected(Platform.ETESYNC) }
        )
        SyncAccount(
            title = R.string.decsync,
            cost = if (hasPro) null else R.string.cost_money,
            description = R.string.decsync_selection_description,
            icon = R.drawable.ic_decsync,
            onClick = { selected(Platform.DECSYNC_CC) }
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun AddAccountDialogPreview() {
    TasksTheme {
        AddAccountDialog(hasTasksAccount = false, hasPro = false, selected = {})
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun AddAccountDialogPreviewWithPro() {
    TasksTheme {
        AddAccountDialog(hasTasksAccount = false, hasPro = true, selected = {})
    }
}