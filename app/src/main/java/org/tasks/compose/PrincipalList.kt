package org.tasks.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.Constants.HALF_KEYLINE
import org.tasks.compose.Constants.ICON_ALPHA
import org.tasks.compose.Constants.KEYLINE_FIRST
import org.tasks.data.Principal
import org.tasks.data.Principal.Companion.name

private val principals = listOf(
    Principal().apply { displayName = "user1" },
    Principal().apply { displayName = "a really really really really really long display name" },
)

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun Owner() = MaterialTheme {
    ListSettingsComposables.PrincipalList(principals) {}
}

@Preview(showBackground = true, backgroundColor = 0x202124)
@Composable
private fun OwnerDark() = MaterialTheme(darkColors()) {
    ListSettingsComposables.PrincipalList(principals) {}
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun NotOwner() = MaterialTheme {
    ListSettingsComposables.PrincipalList(principals, null)
}

object ListSettingsComposables {
    @Composable
    fun PrincipalList(
        principals: List<Principal>,
        onRemove: ((Principal) -> Unit)?,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.list_members),
                style = MaterialTheme.typography.h6,
                color = colors.onBackground
            )
            Spacer(Modifier.height(HALF_KEYLINE))
            principals.forEach {
                PrincipalRow(it, onRemove)
            }
        }
    }

    @Composable
    fun PrincipalRow(
        principal: Principal,
        onRemove: ((Principal) -> Unit)?,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(PaddingValues(0.dp, KEYLINE_FIRST)),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier
                    .width(72.dp - KEYLINE_FIRST),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_perm_identity_24px),
                    contentDescription = null,
                    tint = colors.onBackground,
                    modifier = Modifier
                        .padding(end = KEYLINE_FIRST)
                        .alpha(ICON_ALPHA),
                )
            }
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    principal.name!!,
                    style = MaterialTheme.typography.body1,
                    color = colors.onBackground,
                )
            }
            onRemove?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        modifier = Modifier.then(Modifier.size(24.dp)),
                        onClick = { it(principal) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_clear_24px),
                            contentDescription = null,
                            tint = colors.onBackground,
                            modifier = Modifier.alpha(ICON_ALPHA)
                        )
                    }
                }
            }
        }
    }
}