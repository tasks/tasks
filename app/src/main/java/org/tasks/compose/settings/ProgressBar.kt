package org.tasks.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp

@Composable
fun ProgressBar(showProgress: State<Boolean>) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .requiredHeight(3.dp))
    {
        if (showProgress.value) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                trackColor = LocalContentColor.current.copy(alpha = 0.3f),  //Color.LightGray,
                color = colorResource(org.tasks.kmp.R.color.red_a400)
            )
        }
    }
}
