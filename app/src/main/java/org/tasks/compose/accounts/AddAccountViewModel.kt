package org.tasks.compose.accounts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.data.dao.CaldavDao
import org.tasks.data.newLocalAccount
import org.tasks.extensions.Context.openUri
import org.tasks.sync.AddAccountDialog
import javax.inject.Inject

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val caldavDao: CaldavDao,
) : ViewModel() {
    fun createLocalAccount() = viewModelScope.launch {
        caldavDao.newLocalAccount()
    }

    fun openUrl(context: Context, platform: AddAccountDialog.Platform) {
        val url = when (platform) {
            AddAccountDialog.Platform.DAVX5 -> R.string.url_davx5
            AddAccountDialog.Platform.DECSYNC_CC -> R.string.url_decsync
            AddAccountDialog.Platform.LOCAL -> R.string.help_url_sync
            else -> return
        }
        context.openUri(context.getString(url))
    }
}