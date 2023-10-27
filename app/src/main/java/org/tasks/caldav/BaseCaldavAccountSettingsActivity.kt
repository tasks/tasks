package org.tasks.caldav

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import at.bitfire.dav4jvm.exception.HttpException
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskDeleter
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.compose.ServerSelector
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavAccount.Companion.SERVER_UNKNOWN
import org.tasks.data.CaldavDao
import org.tasks.databinding.ActivityCaldavAccountSettingsBinding
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.Linkify
import org.tasks.extensions.Context.cookiePersistor
import org.tasks.extensions.Context.hideKeyboard
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.addBackPressedCallback
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.security.KeyStoreEncryption
import org.tasks.ui.DisplayableException
import timber.log.Timber
import java.net.ConnectException
import java.net.IDN
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

abstract class BaseCaldavAccountSettingsActivity : ThemedInjectingAppCompatActivity(), Toolbar.OnMenuItemClickListener {
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var encryption: KeyStoreEncryption
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var firebase: Firebase

    protected var caldavAccount: CaldavAccount? = null
    protected lateinit var binding: ActivityCaldavAccountSettingsBinding
    protected lateinit var serverType: MutableState<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaldavAccountSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        caldavAccount = if (savedInstanceState == null) intent.getParcelableExtra(EXTRA_CALDAV_DATA) else savedInstanceState.getParcelable(EXTRA_CALDAV_DATA)
        serverType = mutableStateOf(
            savedInstanceState?.getInt(EXTRA_SERVER_TYPE, SERVER_UNKNOWN)
                ?: caldavAccount?.serverType
                ?: SERVER_UNKNOWN
        )
        if (caldavAccount == null || caldavAccount!!.id == Task.NO_ID) {
            binding.nameLayout.visibility = View.GONE
            binding.description.visibility = View.VISIBLE
            binding.description.setText(description)
            Linkify.safeLinkify(binding.description, android.text.util.Linkify.WEB_URLS)
            serverType.value = SERVER_UNKNOWN
        } else {
            binding.nameLayout.visibility = View.VISIBLE
            binding.description.visibility = View.GONE
            caldavAccount?.error?.takeIf { it.isNotBlank() }?.let {
                binding.description.visibility = View.VISIBLE
                binding.description.setTextColor(ContextCompat.getColor(this, R.color.overdue))
                binding.description.text = getString(R.string.error_adding_account, it)
            }
        }
        if (savedInstanceState == null) {
            caldavAccount?.let {
                binding.name.setText(it.name)
                binding.url.setText(it.url)
                binding.user.setText(it.username)
                if (!isNullOrEmpty(it.password)) {
                    binding.password.setText(PASSWORD_MASK)
                }
                serverType.value = it.serverType
            }
        }
        val toolbar = binding.toolbar.toolbar
        toolbar.title = if (caldavAccount == null) getString(R.string.add_account) else caldavAccount!!.name
        toolbar.navigationIcon = AppCompatResources.getDrawable(this, R.drawable.ic_outline_save_24px)
        toolbar.setNavigationOnClickListener { save() }
        toolbar.inflateMenu(menuRes)
        toolbar.setOnMenuItemClickListener(this)
        toolbar.showOverflowMenu()
        if (caldavAccount == null) {
            toolbar.menu.findItem(R.id.remove).isVisible = false
            binding.name.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.name, InputMethodManager.SHOW_IMPLICIT)
        }
        if (!inventory.hasPro) {
            newSnackbar(getString(R.string.this_feature_requires_a_subscription))
                    .setDuration(BaseTransientBottomBar.LENGTH_INDEFINITE)
                    .setAction(R.string.button_subscribe) {
                        startActivity(Intent(this, PurchaseActivity::class.java))
                    }
                    .show()
        }
        binding.name.addTextChangedListener(
            onTextChanged = { _, _, _, _ -> binding.nameLayout.error = null }
        )
        binding.url.addTextChangedListener(
            onTextChanged = { _, _, _, _ -> binding.urlLayout.error = null }
        )
        binding.user.addTextChangedListener(
            onTextChanged = { _, _, _, _ -> binding.userLayout.error = null }
        )
        binding.password.addTextChangedListener(
            onTextChanged = { _, _, _, _ -> binding.passwordLayout.error = null }
        )
        binding.password.setOnFocusChangeListener { _, hasFocus -> onPasswordFocused(hasFocus) }
        binding.serverSelector.setContent {
            MdcTheme {
                var selected by rememberSaveable { serverType }
                ServerSelector(selected) {
                    serverType.value = it
                    selected = it
                }
            }
        }

        addBackPressedCallback {
            discard()
        }
    }

    @get:StringRes
    protected open val description = 0

    protected open val menuRes = R.menu.menu_caldav_account_settings

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_CALDAV_DATA, caldavAccount)
        outState.putInt(EXTRA_SERVER_TYPE, serverType.value)
    }

    private fun showProgressIndicator() {
        binding.progressBar.progressBar.visibility = View.VISIBLE
    }

    protected fun hideProgressIndicator() {
        binding.progressBar.progressBar.visibility = View.GONE
    }

    private fun requestInProgress(): Boolean {
        return binding.progressBar.progressBar.visibility == View.VISIBLE
    }

    private fun onPasswordFocused(hasFocus: Boolean) {
        if (hasFocus) {
            if (PASSWORD_MASK == binding.password.text.toString()) {
                binding.password.setText("")
            }
        } else if (TextUtils.isEmpty(binding.password.text) && caldavAccount != null) {
            binding.password.setText(PASSWORD_MASK)
        }
    }

    protected val newName: String
        get() {
            val name = binding.name.text.toString().trim { it <= ' ' }
            return if (isNullOrEmpty(name)) newUsername else name
        }

    protected open val newURL: String
        get() = binding.url.text.toString().trim { it <= ' ' }

    protected val newUsername: String
        get() = binding.user.text.toString().trim { it <= ' ' }

    fun passwordChanged(): Boolean {
        return caldavAccount == null || PASSWORD_MASK != binding.password.text.toString().trim { it <= ' ' }
    }

    protected abstract val newPassword: String?

    protected open fun save() = lifecycleScope.launch {
        if (requestInProgress()) {
            return@launch
        }
        val username = newUsername
        val url = newURL
        val password = newPassword
        var failed = false
        if (newName.isBlank()) {
            binding.nameLayout.error = getString(R.string.name_cannot_be_empty)
            failed = true
        }
        if (isNullOrEmpty(url)) {
            binding.urlLayout.error = getString(R.string.url_required)
            failed = true
        } else {
            val baseURL = Uri.parse(url)
            val scheme = baseURL.scheme
            if ("https".equals(scheme, ignoreCase = true) || "http".equals(scheme, ignoreCase = true)) {
                var host = baseURL.host
                if (isNullOrEmpty(host)) {
                    binding.urlLayout.error = getString(R.string.url_host_name_required)
                    failed = true
                } else {
                    try {
                        host = IDN.toASCII(host)
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                    val path = baseURL.encodedPath
                    val port = baseURL.port
                    try {
                        URI(scheme, null, host, port, path, null, null)
                    } catch (e: URISyntaxException) {
                        binding.urlLayout.error = e.localizedMessage
                        failed = true
                    }
                }
            } else {
                binding.urlLayout.error = getString(R.string.url_invalid_scheme)
                failed = true
            }
        }
        if (isNullOrEmpty(username)) {
            binding.userLayout.error = getString(R.string.username_required)
            failed = true
        }
        if (isNullOrEmpty(password)) {
            binding.passwordLayout.error = getString(R.string.password_required)
            failed = true
        }
        when {
            failed -> return@launch
            caldavAccount == null -> {
                showProgressIndicator()
                addAccount(url, username, password!!)
            }
            needsValidation() -> {
                showProgressIndicator()
                updateAccount(url, username, password!!)
            }
            hasChanges() -> {
                updateAccount()
            }
            else -> {
                finish()
            }
        }
    }

    protected abstract suspend fun addAccount(url: String, username: String, password: String)
    protected abstract suspend fun updateAccount(url: String, username: String, password: String)
    protected abstract suspend fun updateAccount()
    protected abstract val helpUrl: Int

    protected fun requestFailed(t: Throwable) {
        hideProgressIndicator()
        when (t) {
            is HttpException ->
                if (t.code == 401)
                    showSnackbar(R.string.invalid_username_or_password)
                else
                    showSnackbar(t.message)
            is DisplayableException -> showSnackbar(t.resId)
            is ConnectException -> showSnackbar(R.string.network_error)
            else -> {
                Timber.e(t)
                showSnackbar(R.string.error_adding_account, t.message!!)
            }
        }
    }

    private fun showSnackbar(resId: Int, vararg formatArgs: Any) {
        showSnackbar(getString(resId, *formatArgs))
    }

    private fun showSnackbar(message: String?) {
        newSnackbar(message).show()
    }

    private fun newSnackbar(message: String?): Snackbar {
        val snackbar = Snackbar.make(binding.rootLayout, message!!, 8000)
                .setTextColor(getColor(R.color.snackbar_text_color))
                .setActionTextColor(getColor(R.color.snackbar_action_color))
        snackbar
                .view
                .setBackgroundColor(getColor(R.color.snackbar_background))
        return snackbar
    }

    protected open fun hasChanges(): Boolean {
        return if (caldavAccount == null) {
            (!isNullOrEmpty(binding.name.text.toString().trim { it <= ' ' })
                    || !isNullOrEmpty(newPassword)
                    || !isNullOrEmpty(binding.url.text.toString().trim { it <= ' ' })
                    || !isNullOrEmpty(newUsername)
                    || serverType.value != SERVER_UNKNOWN
            )
        } else needsValidation() ||
                newName != caldavAccount!!.name ||
                serverType.value != caldavAccount!!.serverType
    }

    protected open fun needsValidation(): Boolean =
            newURL != caldavAccount!!.url
                    || newUsername != caldavAccount!!.username
                    || passwordChanged()

    override fun finish() {
        if (!requestInProgress()) {
            hideKeyboard(binding.name)
            super.finish()
        }
    }

    private fun removeAccountPrompt() {
        if (requestInProgress()) {
            return
        }
        dialogBuilder
                .newDialog()
                .setMessage(R.string.logout_warning)
                .setPositiveButton(R.string.remove) { _, _ -> lifecycleScope.launch { removeAccount() } }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    protected open suspend fun removeAccount() {
        cookiePersistor(caldavAccount?.username).clearSession(caldavAccount?.url)
        taskDeleter.delete(caldavAccount!!)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun discard() {
        if (requestInProgress()) {
            return
        }
        if (hasChanges()) {
            dialogBuilder
                    .newDialog(R.string.discard_changes)
                    .setPositiveButton(R.string.discard) { _, _ -> finish() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        } else {
            finish()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_help -> openUri(helpUrl)
            R.id.remove -> removeAccountPrompt()
        }
        return onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_CALDAV_DATA = "caldavData" // $NON-NLS-1$
        const val EXTRA_SERVER_TYPE = "serverType"
        const val PASSWORD_MASK = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"

        fun CookiePersistor.clearSession(url: String?) {
            val httpUrl = url?.toHttpUrlOrNull() ?: return
            removeAll(loadAll().filter { it.matches(httpUrl) })
        }
    }
}