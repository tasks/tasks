package org.tasks.todoist

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import org.tasks.data.entity.Task
import org.tasks.data.UUIDHelper
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.analytics.Constants
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.getPassword
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TodoistAccountSettingsActivity : BaseCaldavAccountSettingsActivity(), Toolbar.OnMenuItemClickListener {
    @Inject lateinit var clientProvider: TodoistClientProvider

    private val addAccountViewModel: AddTodoistAccountViewModel by viewModels()
    private val updateAccountViewModel: UpdateTodoistAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.serverSelector.visibility = View.GONE
        binding.showAdvanced.visibility = View.VISIBLE
        binding.showAdvanced.setOnCheckedChangeListener { _, _ ->
            updateUrlVisibility()
        }
        updateUrlVisibility()
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) {
            addAccountViewModel.observe(this, this::addAccount, this::requestFailed)
            updateAccountViewModel.observe(this, this::updateAccount, this::requestFailed)
        }
    }

    override fun onPause() {
        super.onPause()
        addAccountViewModel.removeObserver(this)
        updateAccountViewModel.removeObserver(this)
    }

    override val description: Int
        get() = R.string.todoist_account_description

    private suspend fun addAccount(session: String) {
        caldavAccount = CaldavAccount(
            accountType = CaldavAccount.TYPE_TODOIST,
            uuid = UUIDHelper.newUUID(),
        )
        applyTo(caldavAccount!!, session)
    }

    private suspend fun updateAccount(session: String) {
        caldavAccount!!.error = ""
        applyTo(caldavAccount!!, session)
    }

    private suspend fun applyTo(account: CaldavAccount, session: String) {
        hideProgressIndicator()
        account.name = newName
        account.url = newURL
        account.username = newUsername
        if (session != account.getPassword(encryption)) {
            account.password = encryption.encrypt(session)
        }
        saveAccountAndFinish()
    }

    private fun updateUrlVisibility() {
        binding.urlLayout.visibility = if (binding.showAdvanced.isChecked) View.VISIBLE else View.GONE
    }

    override suspend fun addAccount(url: String, username: String, password: String) =
        addAccountViewModel.addAccount(url, username, password)

    override suspend fun updateAccount(url: String, username: String, password: String) =
        updateAccountViewModel.updateAccount(
                url,
                username,
                if (PASSWORD_MASK == password) null else password,
                caldavAccount!!.getPassword(encryption))

    override suspend fun updateAccount() {
        caldavAccount!!.name = newName
        saveAccountAndFinish()
    }

    override val newURL: String
        get() =
            super.newURL
                    .takeIf { it.isNotBlank() }
                    ?: getString(R.string.todoist_url)

    override val newPassword: String
        get() = binding.password.text.toString().trim { it <= ' ' }

    override val helpUrl = R.string.url_todoist

    private suspend fun saveAccountAndFinish() {
        if (caldavAccount!!.id == Task.NO_ID) {
            caldavDao.insert(caldavAccount!!)
            firebase.logEvent(
                    R.string.event_sync_add_account,
                    R.string.param_type to Constants.SYNC_TYPE_TODOIST
            )
        } else {
            caldavDao.update(caldavAccount!!)
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    override suspend fun removeAccount() {
        try {
            caldavAccount?.let { clientProvider.forAccount(it).logout() }
        } catch (e: Exception) {
            Timber.e(e)
        }
        super.removeAccount()
    }
}
