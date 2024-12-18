package org.tasks.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.sp
import org.tasks.R

@Composable
fun SettingsSurface(content: @Composable ColumnScope.() -> Unit) {
    ProvideTextStyle(LocalTextStyle.current.copy(fontSize = 18.sp)) {
        Surface(
            color = colorResource(id = R.color.window_background),
            contentColor = colorResource(id = R.color.text_primary)
        ) {
            Column(modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}
