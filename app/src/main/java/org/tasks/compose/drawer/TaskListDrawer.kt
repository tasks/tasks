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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.tasks.R
import org.tasks.compose.components.SearchBar
import org.tasks.extensions.formatNumber
import org.tasks.filters.FilterImpl
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.themes.TasksTheme

@Composable
fun TaskListDrawer(
    begForMoney: Boolean,
    filters: ImmutableList<DrawerItem>,
    onClick: (DrawerItem) -> Unit,
    onDrawerAction: (DrawerAction) -> Unit,
    onAddClick: (DrawerItem.Header) -> Unit,
    onErrorClick: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val searching by remember (query) {
        derivedStateOf {
            query.isNotBlank()
        }
    }
    var hasFocus by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .imePadding(),
        contentPadding = PaddingValues(bottom = WindowInsets.mandatorySystemGestures
            .asPaddingValues()
            .calculateBottomPadding()),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchBar(
                    modifier = Modifier
                        .onFocusChanged { hasFocus = it.hasFocus }
                        .padding(start = 8.dp, end = if (hasFocus) 8.dp else 0.dp, bottom = 4.dp)
                        .weight(1f)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    ,
                    text = query,
                    onTextChange = { onQueryChange(it) },
                    placeHolder = stringResource(id = R.string.TLA_menu_search),
                    onCloseClicked = { onQueryChange("") },
                    onSearchClicked = {
                        // TODO: close keyboard
                    },
                )
                if (!hasFocus) {
                    if (begForMoney) {
                        IconButton(onClick = { onDrawerAction(DrawerAction.PURCHASE) }) {
                            Icon(
                                imageVector = Icons.Outlined.AttachMoney,
                                contentDescription = stringResource(id = R.string.button_subscribe),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    IconButton(onClick = { onDrawerAction(DrawerAction.HELP_AND_FEEDBACK) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = stringResource(id = R.string.help_and_feedback),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { onDrawerAction(DrawerAction.SETTINGS) }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(id = R.string.TLA_menu_settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
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
        if (!searching) {
            item {
                Divider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun FilterItem(
    item: DrawerItem.Filter,
    onClick: () -> Unit,
) {
    MenuRow(
        modifier = Modifier
            .background(
                if (item.selected)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = .1f)
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
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (item.shareCount > 0) {
            Icon(
                imageVector = when (item.shareCount) {
                    1 -> Icons.Outlined.PermIdentity
                    else -> Icons.Outlined.PeopleOutline
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurface,
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
            color = MaterialTheme.colorScheme.onSurface,
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
            0 -> MaterialTheme.colorScheme.onSurface
            else -> Color(color)
        }
    )
}

@Composable
internal fun HeaderItem(
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
                color = MaterialTheme.colorScheme.onSurface,
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
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (canAdd) {
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (item.hasError) {
                IconButton(onClick = onErrorClick) {
                    Icon(
                        imageVector = Icons.Outlined.SyncProblem,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
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
    TasksTheme {
        TaskListDrawer(
            begForMoney = true,
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
                        )
                    },
                )
            ),
            onClick = {},
            onDrawerAction = {},
            onAddClick = {},
            onErrorClick = {},
            query = "",
            onQueryChange = {},
        )
    }
}
