package org.tasks.caldav

import android.app.Activity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import org.tasks.data.UUIDHelper
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.analytics.Constants
import org.tasks.data.entity.CaldavAccount
import timber.log.Timber

@AndroidEntryPoint
class CaldavAccountSettingsActivity : BaseCaldavAccountSettingsActivity(), Toolbar.OnMenuItemClickListener {
    private val viewModel: CaldavAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.inFlight.observe(this) { binding.progressBar.progressBar.isVisible = it }
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
    }

    override val description: Int
        get() = R.string.caldav_account_description

    private suspend fun addAccount(principal: String) {
        hideProgressIndicator()
        Timber.d("Found principal: %s", principal)
        caldavDao.insert(
            CaldavAccount(
                name = newName,
                url = principal,
                username = newUsername,
                password = encryption.encrypt(newPassword!!),
                uuid = UUIDHelper.newUUID(),
            )
        )
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
        caldavAccount!!.serverType = serverType.value
        if (passwordChanged()) {
            caldavAccount!!.password = encryption.encrypt(newPassword!!)
        }
        caldavDao.update(caldavAccount!!)
        setResult(Activity.RESULT_OK)
        finish()
    }

    override suspend fun addAccount(url: String, username: String, password: String) {
        viewModel.addAccount(url, username, password)?.let { addAccount(it) }
    }

    override suspend fun updateAccount(url: String, username: String, password: String) {
        viewModel.updateCaldavAccount(url, username, password)?.let { updateAccount(it) }
    }

    override suspend fun updateAccount() = updateAccount(caldavAccount!!.url)

    override val newPassword: String?
        get() {
            val input = binding.password.text.toString().trim { it <= ' ' }
            return if (PASSWORD_MASK == input) encryption.decrypt(caldavAccount!!.password) else input
        }

    override val helpUrl = R.string.url_caldav
}