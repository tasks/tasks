package org.tasks.caldav

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.Constants
import org.tasks.compose.ListSettingsComposables.PrincipalList
import org.tasks.compose.ShareInvite.ShareInviteDialog
import org.tasks.data.PrincipalWithAccess
import org.tasks.data.dao.PrincipalDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.themes.TasksTheme
import org.tasks.themes.colorOn
import javax.inject.Inject

@AndroidEntryPoint
class CaldavCalendarSettingsActivity : BaseCaldavCalendarSettingsActivity() {

    @Inject lateinit var principalDao: PrincipalDao

    private val viewModel: CaldavCalendarViewModel by viewModels()

    private var principalsList: MutableState<List<PrincipalWithAccess>> = mutableStateOf( emptyList<PrincipalWithAccess>().toMutableList())
    private val removeDialog = mutableStateOf<PrincipalWithAccess?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.inFlight.observe(this) { baseViewModel.showProgress(it) }

        viewModel.error.observe(this) { throwable ->
            throwable?.let {
                requestFailed(it)
                viewModel.error.value = null
            }
        }
        viewModel.finish.observe(this) {
            setResult(RESULT_OK, it)
            finish()
        }

        setContent {
            TasksTheme {
                BaseCaldavSettingsContent(
                    fab = {
                        if (caldavAccount.canShare && (isNew || caldavCalendar?.access == ACCESS_OWNER)) {
                            val openDialog = rememberSaveable { mutableStateOf(false) }
                            ShareInviteDialog(
                                openDialog,
                                email = caldavAccount.serverType != SERVER_OWNCLOUD
                            ) { input ->
                                lifecycleScope.launch {
                                    share(input)
                                    openDialog.value = false
                                }
                            }
                            FloatingActionButton(
                                onClick = { openDialog.value = true },
                                modifier = Modifier.padding(Constants.KEYLINE_FIRST),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PersonAdd,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                ) {
                    caldavCalendar?.takeIf { it.id > 0 }?.let { calendar->
                        val principals = principalDao.getPrincipals(calendar.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
                        PrincipalList(
                            principals = principals,
                            onRemove = if (canRemovePrincipals) { { onRemove(it) } } else null,
                        )
                    }
                    if (principalsList.value.isNotEmpty())
                        PrincipalList(
                            principalsList.value,
                            onRemove = if (canRemovePrincipals) ::onRemove else null
                        )
                }

                removeDialog.value?.let { principal ->
                    AlertDialog(
                        onDismissRequest = { removeDialog.value = null },
                        confirmButton = {
                            Constants.TextButton(text = R.string.ok) {
                                removePrincipal(principal)
                                removeDialog.value = null
                            }
                        },
                        dismissButton = {
                            Constants.TextButton(text = R.string.cancel) {
                                removeDialog.value = null
                            }
                        },
                        title = {
                            Text(
                                stringResource(id = R.string.remove_user),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(
                                    R.string.remove_user_confirmation,
                                    principal.name,
                                    caldavCalendar?.name ?: ""
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            }
        }
    }

    private val canRemovePrincipals: Boolean
        get() = caldavCalendar?.access == ACCESS_OWNER && caldavAccount.canRemovePrincipal

    private fun onRemove(principal: PrincipalWithAccess) {
        if (requestInProgress()) return
        removeDialog.value = principal
    }

    private fun removePrincipal(principal: PrincipalWithAccess) = lifecycleScope.launch {
        try {
            viewModel.removeUser(caldavAccount, caldavCalendar!!, principal)
        } catch (e: Exception) {
            requestFailed(e)
        }
    }

    override suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int) {
        caldavCalendar = viewModel.createCalendar(caldavAccount, name, color, baseViewModel.icon)
    }

    override suspend fun updateNameAndColor(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        name: String,
        color: Int
    ) {
        viewModel.updateCalendar(account, calendar, name, color, baseViewModel.icon)
    }

    override suspend fun deleteCalendar(
        caldavAccount: CaldavAccount,
        caldavCalendar: CaldavCalendar
    ) {
        viewModel.deleteCalendar(caldavAccount, caldavCalendar)
    }

    private suspend fun share(email: String) {
        if (isNew) {
            viewModel.ignoreFinish = true
            try {
                save()
            } finally {
                viewModel.ignoreFinish = false
            }
        }
        caldavCalendar?.let { viewModel.addUser(caldavAccount, it, email) }
    }

    companion object {
        val CaldavAccount.canRemovePrincipal: Boolean
            get() = when (serverType) {
                SERVER_TASKS, SERVER_OWNCLOUD, SERVER_SABREDAV, SERVER_NEXTCLOUD -> true
                else -> false
            }

        val CaldavAccount.canShare: Boolean
            get() = when (serverType) {
                SERVER_TASKS, SERVER_OWNCLOUD, SERVER_SABREDAV, SERVER_NEXTCLOUD -> true
                else -> false
            }
    }
}