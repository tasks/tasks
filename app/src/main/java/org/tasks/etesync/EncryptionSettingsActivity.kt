package org.tasks.etesync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import at.bitfire.dav4jvm.exception.HttpException
import butterknife.ButterKnife
import butterknife.OnTextChanged
import com.etesync.journalmanager.Constants.Companion.CURRENT_VERSION
import com.etesync.journalmanager.Crypto.CryptoManager
import com.etesync.journalmanager.Crypto.deriveKey
import com.etesync.journalmanager.Exceptions.IntegrityException
import com.etesync.journalmanager.Exceptions.VersionTooNewException
import com.etesync.journalmanager.UserInfoManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.CaldavAccount
import org.tasks.databinding.ActivityEtesyncEncryptionSettingsBinding
import org.tasks.extensions.Context.openUri
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.security.KeyStoreEncryption
import org.tasks.ui.DisplayableException
import java.net.ConnectException
import javax.inject.Inject

@Deprecated("use etebase")
@AndroidEntryPoint
class EncryptionSettingsActivity : ThemedInjectingAppCompatActivity(), Toolbar.OnMenuItemClickListener {
    @Inject lateinit var encryption: KeyStoreEncryption

    private lateinit var binding: ActivityEtesyncEncryptionSettingsBinding
    private var userInfo: UserInfoManager.UserInfo? = null
    private var caldavAccount: CaldavAccount? = null
    private val createUserInfoViewModel: CreateUserInfoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEtesyncEncryptionSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ButterKnife.bind(this)
        val intent = intent
        caldavAccount = intent.getParcelableExtra(EXTRA_ACCOUNT)
        userInfo = intent.getSerializableExtra(EXTRA_USER_INFO) as UserInfoManager.UserInfo
        if (userInfo == null) {
            binding.description.visibility = View.VISIBLE
            binding.repeatEncryptionPasswordLayout.visibility = View.VISIBLE
        }
        val toolbar = binding.toolbar.toolbar
        toolbar.title = if (caldavAccount == null) getString(R.string.add_account) else caldavAccount!!.name
        toolbar.navigationIcon = getDrawable(R.drawable.ic_outline_save_24px)
        toolbar.setNavigationOnClickListener { save() }
        toolbar.inflateMenu(R.menu.menu_help)
        toolbar.setOnMenuItemClickListener(this)
        themeColor.apply(toolbar)
        createUserInfoViewModel.observe(this, { returnDerivedKey(it) }, this::requestFailed)
        if (createUserInfoViewModel.inProgress) {
            showProgressIndicator()
        }
    }

    private fun showProgressIndicator() {
        binding.progressBar.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressIndicator() {
        binding.progressBar.progressBar.visibility = View.GONE
    }

    private fun requestInProgress() = binding.progressBar.progressBar.visibility == View.VISIBLE

    private fun returnDerivedKey(derivedKey: String) {
        hideProgressIndicator()
        val result = Intent()
        result.putExtra(EXTRA_DERIVED_KEY, derivedKey)
        setResult(Activity.RESULT_OK, result)
        finish()
        return
    }

    private fun save() = lifecycleScope.launch {
        if (requestInProgress()) {
            return@launch
        }
        val encryptionPassword = newEncryptionPassword
        val derivedKey = caldavAccount!!.getEncryptionPassword(encryption)
        if (isNullOrEmpty(encryptionPassword) && isNullOrEmpty(derivedKey)) {
            binding.encryptionPasswordLayout.error = getString(R.string.encryption_password_required)
            return@launch
        }
        if (userInfo == null) {
            val repeatEncryptionPassword = binding.repeatEncryptionPassword.text.toString().trim { it <= ' ' }
            if (encryptionPassword != repeatEncryptionPassword) {
                binding.repeatEncryptionPasswordLayout.error = getString(R.string.passwords_do_not_match)
                return@launch
            }
        }
        val key = if (isNullOrEmpty(encryptionPassword)) derivedKey else deriveKey(caldavAccount!!.username!!, encryptionPassword)
        val cryptoManager: CryptoManager
        cryptoManager = try {
            val version = if (userInfo == null) CURRENT_VERSION else userInfo!!.version!!.toInt()
            CryptoManager(version, key, "userInfo")
        } catch (e: VersionTooNewException) {
            requestFailed(e)
            return@launch
        } catch (e: IntegrityException) {
            requestFailed(e)
            return@launch
        }
        if (userInfo == null) {
            showProgressIndicator()
            createUserInfoViewModel.createUserInfo(caldavAccount!!, key)
        } else {
            try {
                userInfo!!.verify(cryptoManager)
                returnDerivedKey(key)
            } catch (e: IntegrityException) {
                binding.encryptionPasswordLayout.error = getString(R.string.encryption_password_wrong)
            }
        }
    }

    private fun requestFailed(t: Throwable) {
        hideProgressIndicator()
        when (t) {
            is HttpException -> showSnackbar(t.message)
            is DisplayableException -> showSnackbar(t.resId)
            is ConnectException -> showSnackbar(R.string.network_error)
            else -> showSnackbar(R.string.error_adding_account, t.message!!)
        }
    }

    private fun showSnackbar(resId: Int, vararg formatArgs: Any) =
            showSnackbar(getString(resId, *formatArgs))

    private fun showSnackbar(message: String?) =
            newSnackbar(message).show()

    private fun newSnackbar(message: String?): Snackbar {
        val snackbar = Snackbar.make(binding.rootLayout, message!!, 8000)
                .setTextColor(getColor(R.color.snackbar_text_color))
                .setActionTextColor(getColor(R.color.snackbar_action_color))
        snackbar
                .view
                .setBackgroundColor(getColor(R.color.snackbar_background))
        return snackbar
    }

    @OnTextChanged(R.id.repeat_encryption_password)
    fun onRpeatEncryptionPasswordChanged() {
        binding.repeatEncryptionPasswordLayout.error = null
    }

    @OnTextChanged(R.id.encryption_password)
    fun onEncryptionPasswordChanged() {
        binding.encryptionPasswordLayout.error = null
    }

    private val newEncryptionPassword: String
        get() = binding.encryptionPassword.text.toString().trim { it <= ' ' }

    override fun finish() {
        if (!requestInProgress()) {
            super.finish()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_help) {
            openUri(R.string.url_etesync)
            true
        } else {
            onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_USER_INFO = "extra_user_info"
        const val EXTRA_ACCOUNT = "extra_account"
        const val EXTRA_DERIVED_KEY = "extra_derived_key"
    }
}