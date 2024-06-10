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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import org.tasks.data.PrincipalWithAccess
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_ACCEPTED
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_DECLINED
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_INVALID
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_NO_RESPONSE
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_UNKNOWN
import org.tasks.data.entity.Principal
import org.tasks.data.entity.PrincipalAccess
import org.tasks.themes.TasksTheme

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
private fun Owner() = TasksTheme {
    ListSettingsComposables.PrincipalList(principals) {}
}

@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun NotOwner() = TasksTheme {
    ListSettingsComposables.PrincipalList(principals, null)
}

object ListSettingsComposables {
    @Composable
    fun PrincipalList(
        principals: List<PrincipalWithAccess>,
        onRemove: ((PrincipalWithAccess) -> Unit)?,
    ) {
        if (principals.isEmpty()) {
            return
        }
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.list_members),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
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
                    tint = MaterialTheme.colorScheme.onSurface,
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
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (principal.inviteStatus) {
                                INVITE_DECLINED, INVITE_INVALID -> colorResource(R.color.overdue)
                                else -> MaterialTheme.colorScheme.onSurface
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
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.alpha(ICON_ALPHA)
                        )
                    }
                }
            }
        }
    }
}