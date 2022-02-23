package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
            modifier = Modifier.padding(bottom = 16.dp).weight(weight = 1f),
        )
        IconButton(
            onClick = remove,
            modifier = Modifier
                .size(40.dp)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_clear_24px),
                modifier = Modifier.alpha(
                    ResourcesCompat.getFloat(
                        LocalContext.current.resources,
                        R.dimen.alpha_secondary
                    )
                ),
                contentDescription = stringResource(id = R.string.delete)
            )
        }
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