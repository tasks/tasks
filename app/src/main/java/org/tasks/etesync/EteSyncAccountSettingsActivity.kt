package org.tasks.etesync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.lifecycle.lifecycleScope
import butterknife.OnCheckedChanged
import com.etesync.journalmanager.Crypto.CryptoManager
import com.etesync.journalmanager.Exceptions.IntegrityException
import com.etesync.journalmanager.Exceptions.VersionTooNewException
import com.etesync.journalmanager.UserInfoManager
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Constants
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.data.CaldavAccount
import timber.log.Timber
import javax.inject.Inject

@Deprecated("use etebase")
@AndroidEntryPoint
class EteSyncAccountSettingsActivity : BaseCaldavAccountSettingsActivity(), Toolbar.OnMenuItemClickListener {
    @Inject lateinit var eteSyncClient: EteSyncClient

    private val addAccountViewModel: AddEteSyncAccountViewModel by viewModels()
    private val updateAccountViewModel: UpdateEteSyncAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.repeat.visibility = View.GONE
        binding.showAdvanced.visibility = View.VISIBLE
        binding.description.visibility = View.VISIBLE
        binding.description.setTextColor(ContextCompat.getColor(this, R.color.overdue))
        binding.description.setText(description)
        updateUrlVisibility()
    }

    override val description = R.string.etesync_deprecated

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

    private suspend fun addAccount(userInfoAndToken: Pair<UserInfoManager.UserInfo, String>) {
        caldavAccount = CaldavAccount()
        caldavAccount!!.accountType = CaldavAccount.TYPE_ETESYNC
        caldavAccount!!.uuid = UUIDHelper.newUUID()
        applyTo(caldavAccount!!, userInfoAndToken)
    }

    private suspend fun updateAccount(userInfoAndToken: Pair<UserInfoManager.UserInfo, String>) {
        caldavAccount!!.error = ""
        applyTo(caldavAccount!!, userInfoAndToken)
    }

    private suspend fun applyTo(account: CaldavAccount, userInfoAndToken: Pair<UserInfoManager.UserInfo, String>) {
        hideProgressIndicator()
        account.name = newName
        account.url = newURL
        account.username = newUsername
        val token = userInfoAndToken.second
        if (token != account.getPassword(encryption)) {
            account.password = encryption.encrypt(token!!)
        }
        val userInfo = userInfoAndToken.first
        if (testUserInfo(userInfo)) {
            saveAccountAndFinish()
        } else {
            val intent = Intent(this, EncryptionSettingsActivity::class.java)
            intent.putExtra(EncryptionSettingsActivity.EXTRA_USER_INFO, userInfo)
            intent.putExtra(EncryptionSettingsActivity.EXTRA_ACCOUNT, account)
            startActivityForResult(intent, REQUEST_ENCRYPTION_PASSWORD)
        }
    }

    private fun testUserInfo(userInfo: UserInfoManager.UserInfo?): Boolean {
        val encryptionKey = caldavAccount!!.getEncryptionPassword(encryption)
        if (userInfo != null && !isNullOrEmpty(encryptionKey)) {
            try {
                val cryptoManager = CryptoManager(userInfo.version!!.toInt(), encryptionKey, "userInfo")
                userInfo.verify(cryptoManager)
                return true
            } catch (e: IntegrityException) {
                Timber.e(e)
            } catch (e: VersionTooNewException) {
                Timber.e(e)
            }
        }
        return false
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
        get() {
            val url = super.newURL
            return if (isNullOrEmpty(url)) getString(R.string.etesync_url) else url
        }

    override val newPassword: String
        get() = binding.password.text.toString().trim { it <= ' ' }

    override val helpUrl = R.string.url_etesync

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENCRYPTION_PASSWORD) {
            if (resultCode == Activity.RESULT_OK) {
                lifecycleScope.launch {
                    val key = data!!.getStringExtra(EncryptionSettingsActivity.EXTRA_DERIVED_KEY)!!
                    caldavAccount!!.encryptionKey = encryption.encrypt(key)
                    saveAccountAndFinish()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private suspend fun saveAccountAndFinish() {
        if (caldavAccount!!.id == Task.NO_ID) {
            caldavDao.insert(caldavAccount!!)
            firebase.logEvent(
                    R.string.event_sync_add_account,
                    R.string.param_type to Constants.SYNC_TYPE_ETESYNC
            )
        } else {
            caldavDao.update(caldavAccount!!)
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    override suspend fun removeAccount() {
        caldavAccount?.let { eteSyncClient.forAccount(it).invalidateToken() }
        super.removeAccount()
    }

    companion object {
        private const val REQUEST_ENCRYPTION_PASSWORD = 10101
    }
}