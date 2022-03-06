package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import org.tasks.R


@Composable
fun AlarmRow(text: String, remove: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .weight(weight = 1f),
        )
        ClearButton(onClick = remove)
    }
}

@Composable
fun ClearButton(onClick: () -> Unit) {
    Icon(
        imageVector = Icons.Outlined.Clear,
        modifier = Modifier
            .padding(12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .alpha(
                ResourcesCompat.getFloat(LocalContext.current.resources, R.dimen.alpha_secondary)
            ),
        contentDescription = stringResource(id = R.string.delete)
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AlarmRowPreview() {
    MaterialTheme(if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        AlarmRow(text = "When due") {

        }
    }
}