package org.tasks.etebase

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import butterknife.OnCheckedChanged
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Constants
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.data.CaldavAccount
import javax.inject.Inject

@AndroidEntryPoint
class EtebaseAccountSettingsActivity : BaseCaldavAccountSettingsActivity(), Toolbar.OnMenuItemClickListener {
    @Inject lateinit var clientProvider: EtebaseClientProvider

    private val addAccountViewModel: AddEtebaseAccountViewModel by viewModels()
    private val updateAccountViewModel: UpdateEtebaseAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.repeat.visibility = View.GONE
        binding.showAdvanced.visibility = View.VISIBLE
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
        get() = R.string.etesync_account_description

    private suspend fun addAccount(session: String) {
        caldavAccount = CaldavAccount()
        caldavAccount!!.accountType = CaldavAccount.TYPE_ETEBASE
        caldavAccount!!.uuid = UUIDHelper.newUUID()
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

    @OnCheckedChanged(R.id.show_advanced)
    fun toggleUrl() {
        updateUrlVisibility()
    }

    private fun updateUrlVisibility() {
        binding.urlLayout.visibility = if (binding.showAdvanced.isChecked) View.VISIBLE else View.GONE
    }

    override fun needsValidation(): Boolean {
        return super.needsValidation() || isNullOrEmpty(caldavAccount!!.encryptionKey)
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
                    ?: getString(R.string.etebase_url)

    override val newPassword: String
        get() = binding.password.text.toString().trim { it <= ' ' }

    override val helpUrl: String
        get() = getString(R.string.url_etesync)

    private suspend fun saveAccountAndFinish() {
        if (caldavAccount!!.id == Task.NO_ID) {
            caldavDao.insert(caldavAccount!!)
            firebase.logEvent(
                    R.string.event_sync_add_account,
                    R.string.param_type to Constants.SYNC_TYPE_ETEBASE
            )
        } else {
            caldavDao.update(caldavAccount!!)
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    override suspend fun removeAccount() {
        caldavAccount?.let { clientProvider.forAccount(it).logout() }
        super.removeAccount()
    }
}