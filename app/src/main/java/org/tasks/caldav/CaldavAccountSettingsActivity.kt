package org.tasks.caldav

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.todoroo.astrid.helper.UUIDHelper
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.data.CaldavAccount
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CaldavAccountSettingsActivity : BaseCaldavAccountSettingsActivity(), Toolbar.OnMenuItemClickListener {
    @Inject lateinit var client: CaldavClient

    private var addCaldavAccountViewModel: AddCaldavAccountViewModel? = null
    private var updateCaldavAccountViewModel: UpdateCaldavAccountViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = ViewModelProvider(this)
        addCaldavAccountViewModel = provider.get(AddCaldavAccountViewModel::class.java)
        updateCaldavAccountViewModel = provider.get(UpdateCaldavAccountViewModel::class.java)
        addCaldavAccountViewModel!!.observe(this, Observer { principal: String -> this.addAccount(principal) }, Observer { t: Throwable? -> requestFailed(t!!) })
        updateCaldavAccountViewModel!!.observe(this, Observer { principal: String? -> this.updateAccount(principal) }, Observer { t: Throwable? -> requestFailed(t!!) })
    }

    override val description: Int
        get() = R.string.caldav_account_description

    private fun addAccount(principal: String) {
        hideProgressIndicator()
        Timber.d("Found principal: %s", principal)
        val newAccount = CaldavAccount()
        newAccount.name = newName
        newAccount.url = principal
        newAccount.username = newUsername
        newAccount.password = encryption.encrypt(newPassword!!)
        newAccount.uuid = UUIDHelper.newUUID()
        newAccount.id = caldavDao.insert(newAccount)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun updateAccount(principal: String?) {
        hideProgressIndicator()
        caldavAccount!!.name = newName
        caldavAccount!!.url = principal
        caldavAccount!!.username = newUsername
        caldavAccount!!.error = ""
        if (passwordChanged()) {
            caldavAccount!!.password = encryption.encrypt(newPassword!!)
        }
        caldavAccount!!.isSuppressRepeatingTasks = binding!!.repeat.isChecked
        caldavDao.update(caldavAccount!!)
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun addAccount(url: String?, username: String?, password: String?) {
        addCaldavAccountViewModel!!.addAccount(client, url, username, password)
    }

    override fun updateAccount(url: String?, username: String?, password: String?) {
        updateCaldavAccountViewModel!!.updateCaldavAccount(client, url, username, password)
    }

    override fun updateAccount() {
        updateAccount(caldavAccount!!.url)
    }

    override val newPassword: String?
        get() {
            val input = binding!!.password.text.toString().trim { it <= ' ' }
            return if (PASSWORD_MASK == input) encryption.decrypt(caldavAccount!!.password) else input
        }

    override val helpUrl: String
        get() = "https://tasks.org/caldav"
}