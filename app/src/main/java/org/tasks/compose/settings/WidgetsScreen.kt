package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tasks.R

data class WidgetItem(
    val widgetId: Int,
    val filterTitle: String,
    val color: Int,
)

@Composable
fun WidgetsScreen(
    widgets: List<WidgetItem>,
    onWidgetClick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        if (widgets.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
            ) {
                widgets.forEachIndexed { index, widget ->
                    SettingsItemCard(position = cardPosition(index, widgets.size)) {
                        val borderColor = colorResource(R.color.text_tertiary)
                        PreferenceRow(
                            title = widget.filterTitle,
                            summary = stringResource(R.string.widget_id, widget.widgetId),
                            showChevron = true,
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .padding(start = SettingsContentPadding)
                                        .size(SettingsIconSize)
                                        .clip(CircleShape)
                                        .background(Color(widget.color))
                                        .border(1.dp, borderColor, CircleShape)
                                )
                            },
                            onClick = { onWidgetClick(widget.widgetId) },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
