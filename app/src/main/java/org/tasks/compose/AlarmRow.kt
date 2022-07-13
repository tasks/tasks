package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AlarmRowPreview() {
    MaterialTheme(if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        AlarmRow(text = "When due") {

        }
    }
}