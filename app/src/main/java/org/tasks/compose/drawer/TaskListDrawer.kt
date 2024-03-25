package org.tasks.compose.drawer

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.api.FilterImpl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.tasks.R
import org.tasks.Tasks.Companion.IS_GENERIC
import org.tasks.extensions.formatNumber
import org.tasks.filters.NavigationDrawerSubheader

@Composable
fun TaskListDrawer(
    bottomPadding: Dp = 0.dp,
    begForMoney: Boolean,
    filters: ImmutableList<DrawerItem>,
    onClick: (DrawerItem) -> Unit,
    onDrawerAction: (DrawerAction) -> Unit,
    onAddClick: (DrawerItem.Header) -> Unit,
    onErrorClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .padding(bottom = bottomPadding)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {
        items(items = filters) {
            when (it) {
                is DrawerItem.Filter -> FilterItem(
                    item = it,
                    onClick = { onClick(it) }
                )
                is DrawerItem.Header -> HeaderItem(
                    item = it,
                    canAdd = it.canAdd,
                    toggleCollapsed = { onClick(it) },
                    onAddClick = { onAddClick(it) },
                    onErrorClick = onErrorClick,
                )
            }
        }
        item {
            Divider(modifier = Modifier.fillMaxWidth())
        }
        if (begForMoney) {
            item {
                MenuAction(
                    icon = R.drawable.ic_outline_attach_money_24px,
                    title = if (IS_GENERIC) R.string.TLA_menu_donate else R.string.name_your_price
                ) {
                    onDrawerAction(DrawerAction.PURCHASE)
                }
            }
        }
        item {
            MenuAction(
                icon = R.drawable.ic_outline_edit_24px,
                title = R.string.manage_drawer
            ) {
                onDrawerAction(DrawerAction.CUSTOMIZE_DRAWER)
            }
        }
        item {
            MenuAction(
                icon = R.drawable.ic_outline_settings_24px,
                title = R.string.TLA_menu_settings
            ) {
                onDrawerAction(DrawerAction.SETTINGS)
            }
        }
        item {
            MenuAction(
                icon = R.drawable.ic_outline_help_outline_24px,
                title = R.string.help_and_feedback
            ) {
                onDrawerAction(DrawerAction.HELP_AND_FEEDBACK)
            }
        }
    }
}

@Composable
private fun FilterItem(
    item: DrawerItem.Filter,
    onClick: () -> Unit,
) {
    MenuRow(
        modifier = Modifier
            .background(
                if (item.selected)
                    MaterialTheme.colors.onSurface.copy(alpha = .1f)
                else
                    Color.Transparent
            )
            .clickable(onClick = onClick),
        onClick = onClick,
    ) {
        if (item.icon != -1) {
            DrawerIcon(icon = item.icon, color = item.color)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = item.title,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (item.shareCount > 0) {
            Icon(
                imageVector = when (item.shareCount) {
                    1 -> Icons.Outlined.PermIdentity
                    else -> Icons.Outlined.PeopleOutline
                },
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface.copy(
                    alpha = ContentAlpha.medium
                ),
            )
        }
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (item.count > 0) {
                val locale = LocalConfiguration.current.locales[0]
                Text(
                    text = locale.formatNumber(item.count),
                    color = MaterialTheme.colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun MenuAction(
    icon: Int,
    title: Int,
    onClick: () -> Unit,
) {
    MenuRow(onClick = onClick) {
        DrawerIcon(icon = icon)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = title),
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DrawerIcon(icon: Int, color: Int = 0) {
    Icon(
        modifier = Modifier.size(24.dp),
        painter = painterResource(id = icon),
        contentDescription = null,
        tint = when (color) {
            0 -> MaterialTheme.colors.onSurface
            else -> Color(color)
        }.copy(alpha = ContentAlpha.medium)
    )
}

@Composable
private fun HeaderItem(
    item: DrawerItem.Header,
    canAdd: Boolean,
    toggleCollapsed: () -> Unit,
    onAddClick: () -> Unit,
    onErrorClick: () -> Unit,
) {
    Column {
        Divider(modifier = Modifier.fillMaxWidth())
        MenuRow(
            padding = PaddingValues(start = 16.dp),
            onClick = toggleCollapsed,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = item.title,
                color = MaterialTheme.colors.onSurface,
            )
            IconButton(onClick = toggleCollapsed) {
                val rotation by animateFloatAsState(
                    targetValue = if (item.collapsed) 0f else 180f,
                    animationSpec = tween(250),
                    label = "arrow rotation",
                )
                Icon(
                    modifier = Modifier.rotate(rotation),
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface,
                )
            }
            if (canAdd) {
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onSurface,
                    )
                }
            }
            if (item.hasError) {
                IconButton(onClick = onErrorClick) {
                    Icon(
                        imageVector = Icons.Outlined.SyncProblem,
                        contentDescription = null,
                        tint = MaterialTheme.colors.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp),
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(48.dp)
            .padding(padding)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MenuPreview() {
    MdcTheme {
        TaskListDrawer(
            filters = persistentListOf(
                DrawerItem.Filter(
                    title = "My Tasks",
                    icon = R.drawable.ic_outline_all_inbox_24px,
                    type = { FilterImpl() },
                ),
                DrawerItem.Header(
                    title = "Filters",
                    collapsed = false,
                    canAdd = true,
                    hasError = false,
                    type = {
                        NavigationDrawerSubheader(
                            null,
                            false,
                            false,
                            NavigationDrawerSubheader.SubheaderType.PREFERENCE,
                            0L,
                            0,
                            null
                        )
                    },
                )
            ),
            onClick = {},
            onDrawerAction = {},
            begForMoney = true,
            onAddClick = {},
            onErrorClick = {},
        )
    }
}
