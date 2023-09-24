package org.tasks.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Tasks
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.compose.throttleLatest
import org.tasks.data.TaskContainer
import org.tasks.data.TaskDao
import org.tasks.data.TaskListQuery.getQuery
import org.tasks.extensions.Context.openUri
import org.tasks.preferences.Preferences
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class TaskListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val taskDao: TaskDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val inventory: Inventory,
    private val firebase: Firebase,
) : ViewModel() {

    data class State(
        val filter: Filter? = null,
        val now: Long = DateUtilities.now(),
        val tasks: List<TaskContainer> = emptyList(),
        val begForSubscription: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            invalidate()
        }
    }

    fun setFilter(filter: Filter) {
        _state.update {
            it.copy(filter = filter)
        }
    }

    fun invalidate() {
        _state.update { it.copy(now = DateUtilities.now()) }
    }

    fun dismissBanner(clickedPurchase: Boolean) {
        _state.update {
            it.copy(begForSubscription = false)
        }
        preferences.lastSubscribeRequest = DateUtilities.now()
        firebase.logEvent(R.string.event_banner_sub, R.string.param_click to clickedPurchase)
        if (clickedPurchase) {
            if (Tasks.IS_GOOGLE_PLAY) {
                context.startActivity(Intent(context, PurchaseActivity::class.java))
            } else {
                preferences.lastSubscribeRequest = DateUtilities.now()
                context.openUri(R.string.url_donate)
            }
        }
    }

    init {
        localBroadcastManager.registerRefreshReceiver(refreshReceiver)

        _state
            .filter { it.filter != null }
            .throttleLatest(333)
            .map { taskDao.fetchTasks { getQuery(preferences, it.filter!!) } }
            .onEach { tasks ->
                _state.update {
                    it.copy(tasks = tasks)
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)

        viewModelScope.launch(Dispatchers.Default) {
            if (!inventory.hasPro && !firebase.subscribeCooldown) {
                _state.update {
                    it.copy(begForSubscription = true)
                }
            }
        }
    }

    override fun onCleared() {
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }
}
