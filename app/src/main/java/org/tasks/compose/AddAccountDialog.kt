package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.sync.AddAccountDialog.Platform

@Composable
fun AddAccountDialog(
    hasTasksAccount: Boolean,
    selected: (Platform) -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (!hasTasksAccount) {
            SyncAccount(
                title = R.string.tasks_org,
                description = R.string.tasks_org_description,
                icon = R.drawable.ic_round_icon,
                onClick = { selected(Platform.TASKS_ORG) }
            )
        }
        SyncAccount(
            title = R.string.gtasks_GPr_header,
            description = R.string.google_tasks_selection_description,
            icon = R.drawable.ic_google,
            onClick = { selected(Platform.GOOGLE_TASKS) }
        )
        if (BuildConfig.DEBUG) {
            SyncAccount(
                title = R.string.microsoft,
                description = R.string.microsoft_selection_description,
                icon = R.drawable.ic_microsoft_tasks,
                onClick = { selected(Platform.MICROSOFT) }
            )
        }
        SyncAccount(
            title = R.string.davx5,
            description = R.string.davx5_selection_description,
            icon = R.drawable.ic_davx5_icon_green_bg,
            onClick = { selected(Platform.DAVX5) }
        )
        SyncAccount(
            title = R.string.caldav,
            description = R.string.caldav_selection_description,
            icon = R.drawable.ic_webdav_logo,
            tint = MaterialTheme.colors.onSurface.copy(
                alpha = ContentAlpha.medium
            ),
            onClick = { selected(Platform.CALDAV) }
        )
        SyncAccount(
            title = R.string.etesync,
            description = R.string.etesync_selection_description,
            icon = R.drawable.ic_etesync,
            onClick = { selected(Platform.ETESYNC) }
        )
        SyncAccount(
            title = R.string.decsync,
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
    MdcTheme {
        AddAccountDialog(hasTasksAccount = false, selected = {})
    }
}