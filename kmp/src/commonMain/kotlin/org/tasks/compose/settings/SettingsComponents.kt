package org.tasks.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.help

val SettingsCardRadius = 20.dp
val SettingsCardInnerRadius = 4.dp
val SettingsCardGap = 2.dp
val SettingsRowPadding = 20.dp
val SettingsContentPadding = 16.dp
val SettingsIconSize = 24.dp
val SettingsSectionGap = 8.dp

enum class CardPosition {
    Only, First, Middle, Last;

    companion object {
        fun forIndex(index: Int, size: Int): CardPosition = when {
            size == 1 -> Only
            index == 0 -> First
            index == size - 1 -> Last
            else -> Middle
        }
    }
}

@Composable
fun SettingsItemCard(
    position: CardPosition = CardPosition.Only,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = when (position) {
        CardPosition.Only -> RoundedCornerShape(SettingsCardRadius)
        CardPosition.First -> RoundedCornerShape(topStart = SettingsCardRadius, topEnd = SettingsCardRadius, bottomStart = SettingsCardInnerRadius, bottomEnd = SettingsCardInnerRadius)
        CardPosition.Middle -> RoundedCornerShape(SettingsCardInnerRadius)
        CardPosition.Last -> RoundedCornerShape(topStart = SettingsCardInnerRadius, topEnd = SettingsCardInnerRadius, bottomStart = SettingsCardRadius, bottomEnd = SettingsCardRadius)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(48.dp)
            .then(
                if (onClick != null)
                    Modifier.clickable(onClick = onClick)
                else
                    Modifier
            ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(Res.string.help),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(SettingsIconSize),
            )
        }
    }
}
