package org.tasks.caldav

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CaldavAccountViewModel @Inject constructor(
    private val provider: CaldavClientProvider
) : CaldavViewModel() {
    suspend fun addAccount(url: String, username: String, password: String): String? =
        doRequest {
            withContext(Dispatchers.IO) {
                provider
                    .forUrl(url, username, password)
                    .homeSet(username, password)
            }
        }

    suspend fun updateCaldavAccount(url: String, username: String, password: String): String? =
        doRequest {
            withContext(Dispatchers.IO) {
                provider.forUrl(url, username, password).homeSet(username, password)
            }
        }
}