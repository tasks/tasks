package org.tasks.caldav

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.analytics.Constants
import org.tasks.data.UUIDHelper
import org.tasks.data.entity.CaldavAccount

@AndroidEntryPoint
class LocalAccountSettingsActivity : BaseCaldavAccountSettingsActivity(), Toolbar.OnMenuItemClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.userLayout.visibility = View.GONE
        binding.passwordLayout.visibility = View.GONE
        binding.urlLayout.visibility = View.GONE
        binding.serverSelector.visibility = View.GONE
    }

    override fun hasChanges() = newName != caldavAccount!!.name

    override fun save() = lifecycleScope.launch {
        if (newName.isBlank()) {
            binding.nameLayout.error = getString(R.string.name_cannot_be_empty)
            return@launch
        }
        updateAccount()
    }

    private suspend fun addAccount() {
        caldavDao.insert(
            CaldavAccount(
                name = newName,
                uuid = UUIDHelper.newUUID(),
            )
        )
        firebase.logEvent(
                R.string.event_sync_add_account,
                R.string.param_type to Constants.SYNC_TYPE_LOCAL
        )
        setResult(Activity.RESULT_OK)
        finish()
    }

    override suspend fun updateAccount() {
        caldavAccount!!.name = newName
        caldavDao.update(caldavAccount!!)
        setResult(Activity.RESULT_OK)
        finish()
    }

    override suspend fun addAccount(url: String, username: String, password: String) {
        addAccount()
    }

    override suspend fun updateAccount(url: String, username: String, password: String) {
        updateAccount()
    }

    override suspend fun removeAccountPrompt() {
        val countTasks = caldavAccount?.uuid?.let { caldavDao.countTasks(it) } ?: 0
        val countString = resources.getQuantityString(R.plurals.task_count, countTasks, countTasks)
        dialogBuilder
            .newDialog()
            .setTitle(
                R.string.delete_tag_confirmation,
                caldavAccount?.name?.takeIf { it.isNotBlank() } ?: getString(R.string.local_lists)
            )
            .apply {
                if (countTasks > 0) {
                    setMessage(R.string.delete_tasks_warning, countString)
                } else {
                    setMessage(R.string.logout_warning)
                }
            }
            .setPositiveButton(R.string.delete) { _, _ -> lifecycleScope.launch { removeAccount() } }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override val newPassword: String? = null

    override val helpUrl = R.string.url_caldav
}