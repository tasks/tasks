package org.tasks.opentasks

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.caldav.BaseCaldavAccountSettingsActivity
import org.tasks.data.OpenTaskDao.Companion.isDavx5

@AndroidEntryPoint
class OpenTaskAccountSettingsActivity : BaseCaldavAccountSettingsActivity(), Toolbar.OnMenuItemClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.userLayout.visibility = View.GONE
        binding.passwordLayout.visibility = View.GONE
        binding.urlLayout.visibility = View.GONE

        if (caldavAccount?.uuid?.isDavx5() != true) {
            binding.repeat.visibility = View.GONE
        }
    }

    override val newPassword = ""

    private suspend fun updateAccount(principal: String?) {
        hideProgressIndicator()
        caldavAccount!!.name = newName
        caldavAccount!!.url = principal
        caldavAccount!!.username = newUsername
        caldavAccount!!.error = ""
        if (passwordChanged()) {
            caldavAccount!!.password = encryption.encrypt(newPassword)
        }
        caldavAccount!!.isSuppressRepeatingTasks = binding.repeat.isChecked
        caldavDao.update(caldavAccount!!)
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun hasChanges() =
            newName != caldavAccount!!.name
                    || binding.repeat.isChecked != caldavAccount!!.isSuppressRepeatingTasks

    override fun save() = lifecycleScope.launch {
        if (newName.isBlank()) {
            binding.nameLayout.error = getString(R.string.name_cannot_be_empty)
            return@launch
        }
        updateAccount()
    }

    override suspend fun addAccount(url: String, username: String, password: String) {}

    override suspend fun updateAccount(url: String, username: String, password: String) {}

    override suspend fun updateAccount() = updateAccount(caldavAccount!!.url)

    override val helpUrl = R.string.help_url_sync

    override val menuRes = R.menu.menu_help
}