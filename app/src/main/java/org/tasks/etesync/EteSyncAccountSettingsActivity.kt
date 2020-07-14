package org.tasks.etesync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.util.Pair
import butterknife.OnCheckedChanged
import com.etesync.journalmanager.Crypto.CryptoManager
import com.etesync.journalmanager.Exceptions.IntegrityException
import com.etesync.journalmanager.Exceptions.VersionTooNewException
import com.etesync.journalmanager.UserInfoManager
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.data.CaldavAccount
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class EteSyncAccountSettingsActivity : BaseCaldavAccountSettingsActivity(), Toolbar.OnMenuItemClickListener {
    @Inject lateinit var eteSyncClient: EteSyncClient

    private val addAccountViewModel: AddEteSyncAccountViewModel by viewModels()
    private val updateAccountViewModel: UpdateEteSyncAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding!!.repeat.visibility = View.GONE
        binding!!.showAdvanced.visibility = View.VISIBLE
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

    private fun addAccount(userInfoAndToken: Pair<UserInfoManager.UserInfo, String>) {
        caldavAccount = CaldavAccount()
        caldavAccount!!.accountType = CaldavAccount.TYPE_ETESYNC
        caldavAccount!!.uuid = UUIDHelper.newUUID()
        applyTo(caldavAccount!!, userInfoAndToken)
    }

    private fun updateAccount(userInfoAndToken: Pair<UserInfoManager.UserInfo, String>) {
        caldavAccount!!.error = ""
        applyTo(caldavAccount!!, userInfoAndToken)
    }

    private fun applyTo(account: CaldavAccount, userInfoAndToken: Pair<UserInfoManager.UserInfo, String>) {
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
        binding!!.urlLayout.visibility = if (binding!!.showAdvanced.isChecked) View.VISIBLE else View.GONE
    }

    override fun needsValidation(): Boolean {
        return super.needsValidation() || isNullOrEmpty(caldavAccount!!.encryptionKey)
    }

    override fun addAccount(url: String, username: String, password: String) {
        addAccountViewModel.addAccount(url, username, password)
    }

    override fun updateAccount(url: String, username: String, password: String?) {
        updateAccountViewModel.updateAccount(
                url,
                username,
                if (PASSWORD_MASK == password) null else password,
                caldavAccount!!.getPassword(encryption))
    }

    override fun updateAccount() {
        caldavAccount!!.name = newName
        saveAccountAndFinish()
    }

    override val newURL: String
        get() {
            val url = super.newURL
            return if (isNullOrEmpty(url)) getString(R.string.etesync_url) else url
        }

    override val newPassword: String
        get() = binding!!.password.text.toString().trim { it <= ' ' }

    override val helpUrl: String
        get() = "https://tasks.org/etesync"

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENCRYPTION_PASSWORD) {
            if (resultCode == Activity.RESULT_OK) {
                val key = data!!.getStringExtra(EncryptionSettingsActivity.EXTRA_DERIVED_KEY)!!
                caldavAccount!!.encryptionKey = encryption.encrypt(key)
                saveAccountAndFinish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun saveAccountAndFinish() {
        if (caldavAccount!!.id == Task.NO_ID) {
            caldavDao.insert(caldavAccount!!)
        } else {
            caldavDao.update(caldavAccount!!)
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun removeAccount() {
        if (caldavAccount != null) {
            Completable.fromAction { eteSyncClient.forAccount(caldavAccount!!).invalidateToken() }
                    .subscribeOn(Schedulers.io())
                    .subscribe()
        }
        super.removeAccount()
    }

    companion object {
        private const val REQUEST_ENCRYPTION_PASSWORD = 10101
    }
}