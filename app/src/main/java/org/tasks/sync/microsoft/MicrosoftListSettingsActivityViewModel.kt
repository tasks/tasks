package org.tasks.sync.microsoft

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.RequestBody.Companion.toRequestBody
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.http.HttpClientFactory
import org.tasks.http.HttpClientFactory.Companion.MEDIA_TYPE_JSON
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class MicrosoftListSettingsActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val httpClientFactory: HttpClientFactory,
    private val caldavDao: CaldavDao,
    private val taskDeleter: TaskDeleter,
) : ViewModel() {
    data class ViewState(
        val requestInFlight: Boolean = false,
        val result: CaldavCalendar? = null,
        val error: Throwable? = null,
        val deleted: Boolean = false,
    )

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()

    private val account: CaldavAccount =
        savedStateHandle[BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_ACCOUNT]!!

    val list: CaldavCalendar? =
        savedStateHandle[BaseCaldavCalendarSettingsActivity.EXTRA_CALDAV_CALENDAR]

    suspend fun createList(displayName: String) {
        _viewState.update { it.copy(requestInFlight = true) }
        val microsoftService = httpClientFactory.getMicrosoftService(account)
        val taskList = TaskLists.TaskList(displayName = displayName)
        val body = Json.encodeToString(taskList).toRequestBody(MEDIA_TYPE_JSON)
        val result = microsoftService.createList(body)
        if (result.isSuccessful) {
            val list = CaldavCalendar(
                account = this@MicrosoftListSettingsActivityViewModel.account.uuid
            ).apply {
                result.body()?.applyTo(this)
            }
            caldavDao.insert(list)
            _viewState.update { it.copy(result = list) }
        } else {
            requestFailed(result)
        }
    }

    suspend fun deleteList() {
        _viewState.update { it.copy(requestInFlight = true) }
        val microsoftService = httpClientFactory.getMicrosoftService(account)
        val result = microsoftService.deleteList(list?.uuid!!)
        if (result.isSuccessful) {
            taskDeleter.delete(list)
            _viewState.update { it.copy(deleted = true) }
        } else {
            requestFailed(result)
        }
    }

    suspend fun updateList(displayName: String) {
        _viewState.update { it.copy(requestInFlight = true) }
        val microsoftService = httpClientFactory.getMicrosoftService(account)
        val taskList = TaskLists.TaskList(displayName = displayName)
        val body = Json.encodeToString(taskList).toRequestBody(MEDIA_TYPE_JSON)
        val result = microsoftService.updateList(list?.uuid!!, body)
        if (result.isSuccessful) {
            result.body()?.applyTo(list)
            caldavDao.update(list)
            _viewState.update { it.copy(result = list) }
        } else {
            requestFailed(result)
        }
    }

    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }

    private fun <T> requestFailed(result: Response<T>) {
        _viewState.update {
            it.copy(
                requestInFlight = false,
                error = HttpException(result),
            )
        }
    }
}