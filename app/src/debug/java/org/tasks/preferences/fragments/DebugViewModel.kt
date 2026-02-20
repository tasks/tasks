package org.tasks.preferences.fragments

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.data.createDueDate
import org.tasks.data.entity.Task
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class DebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inventory: Inventory,
    private val billingClient: BillingClient,
    private val preferences: Preferences,
    private val taskCreator: com.todoroo.astrid.service.TaskCreator,
    private val taskDao: com.todoroo.astrid.dao.TaskDao,
) : ViewModel() {

    var leakCanaryEnabled by mutableStateOf(false)
        private set
    var strictModeVmEnabled by mutableStateOf(false)
        private set
    var strictModeThreadEnabled by mutableStateOf(false)
        private set
    var crashOnViolationEnabled by mutableStateOf(false)
        private set
    var unlockProEnabled by mutableStateOf(false)
        private set
    var iapTitle by mutableStateOf("")
        private set
    var showRestartDialog by mutableStateOf(false)
        private set

    fun refreshState() {
        leakCanaryEnabled = preferences.getBoolean(R.string.p_leakcanary, false)
        strictModeVmEnabled = preferences.getBoolean(R.string.p_strict_mode_vm, false)
        strictModeThreadEnabled = preferences.getBoolean(R.string.p_strict_mode_thread, false)
        crashOnViolationEnabled = preferences.getBoolean(R.string.p_crash_main_queries, false)
        unlockProEnabled = preferences.getBoolean(R.string.p_debug_pro, false)
        refreshIapTitle()
    }

    fun updateLeakCanary(enabled: Boolean) {
        preferences.setBoolean(R.string.p_leakcanary, enabled)
        leakCanaryEnabled = enabled
        showRestartDialog = true
    }

    fun updateStrictModeVm(enabled: Boolean) {
        preferences.setBoolean(R.string.p_strict_mode_vm, enabled)
        strictModeVmEnabled = enabled
        showRestartDialog = true
    }

    fun updateStrictModeThread(enabled: Boolean) {
        preferences.setBoolean(R.string.p_strict_mode_thread, enabled)
        strictModeThreadEnabled = enabled
        showRestartDialog = true
    }

    fun updateCrashOnViolation(enabled: Boolean) {
        preferences.setBoolean(R.string.p_crash_main_queries, enabled)
        crashOnViolationEnabled = enabled
        showRestartDialog = true
    }

    fun updateUnlockPro(enabled: Boolean) {
        preferences.setBoolean(R.string.p_debug_pro, enabled)
        unlockProEnabled = enabled
    }

    fun dismissRestartDialog() {
        showRestartDialog = false
    }

    fun toggleIap(activity: android.app.Activity, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (inventory.getPurchase(Inventory.SKU_THEMES) == null) {
                billingClient.initiatePurchaseFlow(
                    activity,
                    "inapp",
                    Inventory.SKU_THEMES,
                )
            } else {
                billingClient.consume(Inventory.SKU_THEMES)
            }
            refreshIapTitle()
            onComplete()
        }
    }

    fun clearHints() {
        preferences.installDate = min(
            preferences.installDate,
            currentTimeMillis() - TimeUnit.DAYS.toMillis(14),
        )
        preferences.lastSubscribeRequest = 0L
        preferences.lastReviewRequest = 0L
        preferences.shownBeastModeHint = false
        preferences.warnMicrosoft = true
        preferences.warnGoogleTasks = true
        preferences.setBoolean(R.string.p_just_updated, true)
        preferences.setBoolean(R.string.p_local_list_banner_dismissed, false)
        preferences.warnAlarmsDisabled = true
        preferences.warnNotificationsDisabled = true
    }

    fun createTasks(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = 5000
            for (i in 1..count) {
                val task = taskCreator.createWithValues("")
                taskDao.createNew(task)
                task.title = "Task ${task.id}"
                task.dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY,
                    currentTimeMillis(),
                )
                taskDao.save(task)
            }
            onComplete(count)
        }
    }

    private fun refreshIapTitle() {
        val sku = Inventory.SKU_THEMES
        iapTitle = if (inventory.getPurchase(sku) == null) {
            context.getString(R.string.debug_purchase, sku)
        } else {
            context.getString(R.string.debug_consume, sku)
        }
    }
}
