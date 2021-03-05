package org.tasks.caldav

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.CaldavAccount.Companion.SERVER_TASKS
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.Principal
import org.tasks.data.Principal.Companion.name
import org.tasks.data.PrincipalDao
import javax.inject.Inject

@AndroidEntryPoint
class CaldavCalendarSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    @Inject lateinit var principalDao: PrincipalDao

    private val viewModel: CaldavCalendarViewModel by viewModels()

    private val createCalendarViewModel: CreateCalendarViewModel by viewModels()
    private val deleteCalendarViewModel: DeleteCalendarViewModel by viewModels()
    private val updateCalendarViewModel: UpdateCalendarViewModel by viewModels()

    override val layout = R.layout.activity_caldav_calendar_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.inFlight.observe(this) { progressView.isVisible = it }
        viewModel.error.observe(this) { throwable ->
            throwable?.let {
                requestFailed(it)
                viewModel.error.value = null
            }
        }

        createCalendarViewModel.observe(this, this::createSuccessful, this::requestFailed)
        deleteCalendarViewModel.observe(this, this::onDeleted, this::requestFailed)
        updateCalendarViewModel.observe(
                this,
                { updateCalendar() },
                this::requestFailed)

        caldavCalendar?.takeIf { it.id > 0 }?.let {
            principalDao.getPrincipals(it.id).observe(this) {
                findViewById<ComposeView>(R.id.people)
                    .apply { isVisible = it.isNotEmpty() }
                    .setContent { PrincipalList(it) }
            }
        }
    }

    private val canRemovePrincipals: Boolean
        get() = caldavCalendar?.access == ACCESS_OWNER && caldavAccount.canRemovePrincipal

    private fun onRemove(principal: Principal) {
        if (requestInProgress()) {
            return
        }
        dialogBuilder
            .newDialog(R.string.remove_user)
            .setMessage(R.string.remove_user_confirmation, principal.name, caldavCalendar?.name)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ -> removePrincipal(principal) }
            .show()
    }

    private fun removePrincipal(principal: Principal) = lifecycleScope.launch {
        try {
            viewModel.remove(caldavAccount, caldavCalendar!!, principal)
        } catch (e: Exception) {
            requestFailed(e)
        }
    }

    @Composable
    private fun PrincipalList(principals: List<Principal>) {
        tasksTheme.TasksTheme {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.list_members),
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onBackground
                )
                Spacer(Modifier.height(8.dp))
                principals.forEach {
                    PrincipalRow(it)
                }
            }
        }
    }

    @Composable
    fun PrincipalRow(principal: Principal) {
        Row(modifier = Modifier
            .padding(PaddingValues(0.dp, 16.dp))
            .fillMaxWidth()) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_perm_identity_24px),
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp),
                tint = colorResource(R.color.icon_tint_with_alpha)
            )
            Text(
                principal.name!!,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onBackground,
            )
            if (canRemovePrincipals) {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        modifier = Modifier.then(Modifier.size(24.dp)),
                        onClick = { onRemove(principal) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_clear_24px),
                            contentDescription = null,
                            tint = colorResource(R.color.icon_tint_with_alpha),
                        )
                    }
                }
            }
        }
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) =
            createCalendarViewModel.createCalendar(caldavAccount, name, color)

    override suspend fun updateNameAndColor(
            account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int) =
            updateCalendarViewModel.updateCalendar(account, calendar, name, color)

    override suspend fun deleteCalendar(caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar) =
            deleteCalendarViewModel.deleteCalendar(caldavAccount, caldavCalendar)

    @Preview
    @Composable
    private fun PreviewList() {
        PrincipalList(listOf(
            Principal().apply { displayName = "user1" },
            Principal().apply { displayName = "user2" },
        ))
    }

    companion object {
        val CaldavAccount.canRemovePrincipal: Boolean
            get() = when (serverType) {
                SERVER_TASKS, SERVER_OWNCLOUD, SERVER_SABREDAV -> true
                else -> false
            }
    }
}