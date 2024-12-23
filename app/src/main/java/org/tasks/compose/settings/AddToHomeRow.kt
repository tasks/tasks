package org.tasks.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutManagerCompat
import org.tasks.R
import org.tasks.compose.Constants
import org.tasks.kmp.org.tasks.compose.settings.SettingRow
import org.tasks.themes.TasksTheme

@Composable
fun AddToHomeRow(onClick: () -> Unit) {
    val context = LocalContext.current
    val isRequestPinShortcutSupported = LocalInspectionMode.current || remember {
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    }
    if (isRequestPinShortcutSupported) {
        SettingRow(
            modifier = Modifier.clickable(onClick = onClick),
            left = {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = null,
                        tint = colorResource(R.color.icon_tint_with_alpha),
                    )
                }
            },
            center = {
                Text(
                    text = stringResource(R.string.add_to_home_screen),
                    modifier = Modifier.padding(start = Constants.KEYLINE_FIRST)
                )
            }
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun AddToHomePreview() {
    TasksTheme {
        AddToHomeRow(onClick = {})
    }
}
