package org.tasks.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.Constants.HALF_KEYLINE
import org.tasks.compose.Constants.ICON_ALPHA
import org.tasks.compose.Constants.KEYLINE_FIRST
import org.tasks.data.CaldavCalendar.Companion.INVITE_ACCEPTED
import org.tasks.data.CaldavCalendar.Companion.INVITE_DECLINED
import org.tasks.data.CaldavCalendar.Companion.INVITE_INVALID
import org.tasks.data.CaldavCalendar.Companion.INVITE_NO_RESPONSE
import org.tasks.data.CaldavCalendar.Companion.INVITE_UNKNOWN
import org.tasks.data.Principal
import org.tasks.data.PrincipalAccess
import org.tasks.data.PrincipalWithAccess

private val principals = listOf(
    PrincipalWithAccess(
        PrincipalAccess(list = 0, invite = INVITE_INVALID),
        Principal(account = 0, href = "", displayName = "user1")
    ),
    PrincipalWithAccess(
        PrincipalAccess(list = 0, invite = INVITE_ACCEPTED),
        Principal(account = 0, href = "", displayName = "a really really really really really long display name")
    )
)

@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun Owner() = MaterialTheme {
    ListSettingsComposables.PrincipalList(principals) {}
}

@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun NotOwner() = MaterialTheme {
    ListSettingsComposables.PrincipalList(principals, null)
}

object ListSettingsComposables {
    @Composable
    fun PrincipalList(
        principals: List<PrincipalWithAccess>,
        onRemove: ((PrincipalWithAccess) -> Unit)?,
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
        principal: PrincipalWithAccess,
        onRemove: ((PrincipalWithAccess) -> Unit)?,
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
                Column {
                    Text(
                        principal.name,
                        style = MaterialTheme.typography.body1,
                        color = colors.onBackground,
                    )
                    if (principal.inviteStatus != INVITE_ACCEPTED) {
                        Text(
                            stringResource(when (principal.inviteStatus) {
                                INVITE_UNKNOWN, INVITE_NO_RESPONSE ->
                                    R.string.invite_awaiting_response
                                INVITE_DECLINED ->
                                    R.string.invite_declined
                                INVITE_INVALID ->
                                    R.string.invite_invalid
                                else -> throw IllegalStateException()
                            }),
                            style = MaterialTheme.typography.body2,
                            color = when (principal.inviteStatus) {
                                INVITE_DECLINED, INVITE_INVALID -> colorResource(R.color.overdue)
                                else -> colors.onBackground
                            },
                        )
                    }
                }
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