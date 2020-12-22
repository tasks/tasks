package org.tasks.caldav

import android.app.Activity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.analytics.Constants
import org.tasks.data.CaldavAccount
import timber.log.Timber

@AndroidEntryPoint
class CaldavAccountSettingsActivity : BaseCaldavAccountSettingsActivity(), Toolbar.OnMenuItemClickListener {
    private val addCaldavAccountViewModel: AddCaldavAccountViewModel by viewModels()
    private val updateCaldavAccountViewModel: UpdateCaldavAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addCaldavAccountViewModel.observe(this, this::addAccount, this::requestFailed)
        updateCaldavAccountViewModel.observe(this, this::updateAccount, this::requestFailed)
    }

    override val description: Int
        get() = R.string.caldav_account_description

    private suspend fun addAccount(principal: String) {
        hideProgressIndicator()
        Timber.d("Found principal: %s", principal)
        val newAccount = CaldavAccount().apply {
            name = newName
            url = principal
            username = newUsername
            password = encryption.encrypt(newPassword!!)
            uuid = UUIDHelper.newUUID()
            id = caldavDao.insert(this)
        }
        firebase.logEvent(
                R.string.event_sync_add_account,
                R.string.param_type to Constants.SYNC_TYPE_CALDAV
        )
        setResult(Activity.RESULT_OK)
        finish()
    }

    private suspend fun updateAccount(principal: String?) {
        hideProgressIndicator()
        caldavAccount!!.name = newName
        caldavAccount!!.url = principal
        caldavAccount!!.username = newUsername
        caldavAccount!!.error = ""
        if (passwordChanged()) {
            caldavAccount!!.password = encryption.encrypt(newPassword!!)
        }
        caldavAccount!!.isSuppressRepeatingTasks = binding.repeat.isChecked
        caldavDao.update(caldavAccount!!)
        setResult(Activity.RESULT_OK)
        finish()
    }

    override suspend fun addAccount(url: String, username: String, password: String) =
            addCaldavAccountViewModel.addAccount(url, username, password)

    override suspend fun updateAccount(url: String, username: String, password: String) =
            updateCaldavAccountViewModel.updateCaldavAccount(url, username, password)

    override suspend fun updateAccount() =
            updateAccount(caldavAccount!!.url)

    override val newPassword: String?
        get() {
            val input = binding.password.text.toString().trim { it <= ' ' }
            return if (PASSWORD_MASK == input) encryption.decrypt(caldavAccount!!.password) else input
        }

    override val helpUrl: String
        get() = getString(R.string.url_caldav)
}