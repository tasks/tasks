package org.tasks.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.tasks.R

@Composable
fun Toaster(state: SnackbarHostState) {
    SnackbarHost(state) { data ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Snackbar(
                modifier = Modifier.padding(horizontal = 24.dp),
                shape = RoundedCornerShape(10.dp),
                containerColor = colorResource(id = R.color.snackbar_background),
                contentColor = colorResource(id = R.color.snackbar_text_color),
            ) {
                Text(text = data.visuals.message, fontSize = 18.sp)
            }
        }
    }
}
