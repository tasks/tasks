package org.tasks.etesync

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.caldav.BaseCaldavAccountSettingsActivity

@Deprecated("use etebase")
@AndroidEntryPoint
class EteSyncAccountSettingsActivity : BaseCaldavAccountSettingsActivity(), Toolbar.OnMenuItemClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.repeat.visibility = View.GONE
        binding.showAdvanced.visibility = View.GONE
        binding.description.visibility = View.VISIBLE
        binding.description.setTextColor(ContextCompat.getColor(this, R.color.overdue))
        binding.description.setText(description)
        binding.showAdvanced.setOnCheckedChangeListener { _, _ -> updateUrlVisibility() }
        updateUrlVisibility()
    }

    override val description = R.string.etesync_deprecated

    private fun updateUrlVisibility() {
        binding.urlLayout.visibility = View.GONE
    }

    override fun needsValidation() = false

    override fun hasChanges() = false

    override suspend fun addAccount(url: String, username: String, password: String) {}

    override suspend fun updateAccount(url: String, username: String, password: String) {}

    override suspend fun updateAccount() {}

    override val newURL: String
        get() {
            val url = super.newURL
            return if (isNullOrEmpty(url)) getString(R.string.etesync_url) else url
        }

    override val newPassword: String
        get() = binding.password.text.toString().trim { it <= ' ' }

    override val helpUrl = R.string.url_etesync
}