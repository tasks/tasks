package org.tasks.kmp.org.tasks.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingRow(
    left: @Composable () -> Unit,
    center: @Composable () -> Unit,
    right: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box (modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            left()
        }
        Box (
            modifier = Modifier
                .height(56.dp)
                .weight(1f), contentAlignment = Alignment.CenterStart
        ) {
            center()
        }
        right?.let {
            Box (modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                it.invoke()
            }
        }
    }
}
