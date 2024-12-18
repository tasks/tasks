package org.tasks.compose.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.tasks.R
import org.tasks.compose.Constants
import org.tasks.compose.DeleteButton
import org.tasks.themes.TasksTheme

@Composable
fun Toolbar(
    title: String,
    save: () -> Unit,
    optionButton: @Composable () -> Unit,
) {

    /*  Hady: reminder for the future
        val activity = LocalView.current.context as Activity
        activity.window.statusBarColor = colorResource(id = R.color.drawer_color_selected).toArgb()
    */

    Surface(
        shadowElevation = 4.dp,
        color = colorResource(id = R.color.content_background),
        contentColor = colorResource(id = R.color.text_primary),
        modifier = Modifier.requiredHeight(56.dp)
    )
    {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = save, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_outline_save_24px),
                    contentDescription = stringResource(id = R.string.save),
                )
            }
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                modifier = Modifier
                    .weight(0.9f)
                    .padding(start = Constants.KEYLINE_FIRST),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            optionButton()
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun TitleBarPreview() {
    TasksTheme {
        Toolbar(
            title = "Toolbar title",
            save = { /*TODO*/ },
            optionButton = { DeleteButton {} }
        )
    }
}
