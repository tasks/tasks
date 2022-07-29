package org.tasks.compose

import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import org.tasks.R

@Composable
fun DeleteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            modifier = Modifier.alpha(ContentAlpha.medium),
            contentDescription = stringResource(id = R.string.delete)
        )
    }
}
