package org.tasks.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@ExperimentalAnimationApi
@Composable
fun AnimatedBanner(
    visible: Boolean,
    title: String,
    body: String,
    dismissText: String,
    onDismiss: () -> Unit,
    action: String,
    onAction: () -> Unit,
) {
    AnimatedBanner(
        visible = visible,
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        buttons = {
            BannerTextButton(text = dismissText, onDismiss)
            BannerTextButton(text = action, onAction)
        }
    )
}

@ExperimentalAnimationApi
@Composable
private fun AnimatedBanner(
    visible: Boolean,
    content: @Composable () -> Unit,
    buttons: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            content()
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                buttons()
            }
        }
    }
}

@Composable
fun BannerTextButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
        )
    }
}
