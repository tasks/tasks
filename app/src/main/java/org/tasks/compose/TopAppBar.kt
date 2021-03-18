package org.tasks.compose

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.R
import org.tasks.compose.TopAppBar.TopAppBar
import org.tasks.themes.ThemeColor

@Preview
@Composable
private fun DarkAppBar() {
    TopAppBar(
        title = R.string.upgrade_to_pro,
        icon = R.drawable.ic_outline_arrow_back_24px,
        color = ThemeColor(LocalContext.current, 0),
        onClickNavigation = {},
    )
}

@Preview
@Composable
private fun LightAppBar() {
    TopAppBar(
        title = R.string.BFE_Active,
        icon = R.drawable.ic_outline_menu_24px,
        color = ThemeColor(LocalContext.current, -1),
        onClickNavigation = {},
    )
}

object TopAppBar {
    @Composable
    fun TopAppBar(
        title: Int,
        icon: Int,
        color: ThemeColor,
        onClickNavigation: () -> Unit = {}
    ) {
        androidx.compose.material.TopAppBar(
            title = {
                Text(stringResource(title))
            },
            backgroundColor = Color(color.primaryColor),
            contentColor = Color(color.colorOnPrimary),
            navigationIcon = {
                IconButton(
                    onClick = onClickNavigation,
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = stringResource(R.string.back),
                        tint = Color(color.colorOnPrimary),
                    )
                }
            },
        )
    }
}