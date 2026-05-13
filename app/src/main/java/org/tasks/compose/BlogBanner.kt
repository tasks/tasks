package org.tasks.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.tasks.R
import org.tasks.compose.components.Banner

@Composable
fun BlogBanner(
    title: String,
    body: String,
    readMore: () -> Unit,
    dismiss: () -> Unit,
) {
    Banner(
        title = title,
        body = body,
        dismissText = stringResource(id = R.string.dismiss),
        onDismiss = dismiss,
        action = stringResource(id = R.string.read_more),
        onAction = readMore,
    )
}
