package org.tasks.compose.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.NotInterested
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.Constants
import org.tasks.kmp.org.tasks.compose.settings.SettingRow
import org.tasks.themes.TasksTheme

@Composable
fun SelectColorRow(color: State<Color>, selectColor: () -> Unit, clearColor: () -> Unit) =
    SettingRow(
        modifier = Modifier.clickable(onClick =  selectColor),
        left = {
            IconButton(onClick = { selectColor() }) {
                if (color.value == Color.Unspecified) {
                    Icon(
                        imageVector = Icons.Outlined.NotInterested,
                        tint = colorResource(R.color.icon_tint_with_alpha),
                        contentDescription = null
                    )
                } else {
                    val borderColor = colorResource(R.color.icon_tint_with_alpha)  // colorResource(R.color.text_tertiary)
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            drawCircle(color = color.value)
                            drawCircle(color = borderColor, style = Stroke(width = 4.0f)
                            )
                        }
                    }
                }
            }
        },
        center = {
            Text(
                text = LocalContext.current.getString(R.string.color),
                modifier = Modifier.padding(start = Constants.KEYLINE_FIRST)
            )
        },
        right = {
            if (color.value != Color.Unspecified) {
                IconButton(onClick = clearColor) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = null
                    )
                }
            }
        }
    )

@Composable
@Preview(showBackground = true)
private fun ColorSelectPreview () {
    TasksTheme {
        SelectColorRow(
            color = remember { mutableStateOf(Color.Red) },
            selectColor = {},
            clearColor = {}
        )
    }
}
