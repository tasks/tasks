package org.tasks.caldav

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import at.bitfire.dav4jvm.exception.HttpException
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.service.TaskDeleter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.activities.BaseListSettingsActivity
import org.tasks.compose.DeleteButton
import org.tasks.compose.settings.Toaster
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.themes.TasksIcons
import org.tasks.ui.DisplayableException
import java.net.ConnectException
import javax.inject.Inject

abstract class BaseCaldavCalendarSettingsActivity : BaseListSettingsActivity() {
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var taskDeleter: TaskDeleter

    protected var caldavCalendar: CaldavCalendar? = null

    protected lateinit var caldavAccount: CaldavAccount
    override val defaultIcon = TasksIcons.LIST

    protected val snackbar = SnackbarHostState() // to be used by descendants

    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = intent
        caldavCalendar = intent.getParcelableExtra(EXTRA_CALDAV_CALENDAR)
        super.onCreate(savedInstanceState)
        caldavAccount = if (caldavCalendar == null) {
            intent.getParcelableExtra(EXTRA_CALDAV_ACCOUNT)!!
        } else {
            runBlocking { caldavDao.getAccountByUuid(caldavCalendar!!.account!!)!! }
        }
        if (savedInstanceState == null) {
            if (caldavCalendar != null) {
                textState.value = caldavCalendar!!.name ?: ""
                selectedColor = caldavCalendar!!.color
                selectedIcon.value = caldavCalendar?.icon ?: defaultIcon
            }
        }
        updateTheme()
    }

    override val filter: Filter?
        get() = caldavCalendar?.let { CaldavFilter(it) }

    override val toolbarTitle: String
        get() = if (isNew) getString(R.string.new_list) else caldavCalendar!!.name ?: ""

    override suspend fun save() {
        if (requestInProgress()) {
            return
        }
        val name = newName
        if (isNullOrEmpty(name)) {
            errorState.value = getString(R.string.name_cannot_be_empty)
            return
        }
        when {
            caldavCalendar == null -> {
                showProgressIndicator()
                createCalendar(caldavAccount, name, selectedColor)
            }
            nameChanged() || colorChanged() -> {
                showProgressIndicator()
                updateNameAndColor(caldavAccount, caldavCalendar!!, name, selectedColor)
            }
            iconChanged() -> updateCalendar()
            else -> finish()
        }
    }

    protected abstract suspend fun createCalendar(caldavAccount: CaldavAccount, name: String, color: Int)

    protected abstract suspend fun updateNameAndColor(
        account: CaldavAccount, calendar: CaldavCalendar, name: String, color: Int)

    protected abstract suspend fun deleteCalendar(
        caldavAccount: CaldavAccount, caldavCalendar: CaldavCalendar
    )

    private fun showProgressIndicator() { showProgress.value = true }

    private fun hideProgressIndicator() { showProgress.value = false }

    protected fun requestInProgress(): Boolean = showProgress.value

    protected fun requestFailed(t: Throwable) {
        hideProgressIndicator()
        when (t) {
            is HttpException -> showSnackbar(t.message)
            is retrofit2.HttpException -> showSnackbar(t.message() ?: "HTTP ${t.code()}")
            is DisplayableException -> showSnackbar(t.resId)
            is ConnectException -> showSnackbar(R.string.network_error)
            else -> showSnackbar(R.string.error_adding_account, t.message!!)
        }
        return
    }

    private fun showSnackbar(resId: Int, vararg formatArgs: Any) {
        lifecycleScope.launch { snackbar.showSnackbar( getString(resId, *formatArgs) ) }
    }

    private fun showSnackbar(message: String?) {
        lifecycleScope.launch { snackbar.showSnackbar( message!! ) }
    }

    protected suspend fun createSuccessful(url: String?) {
        val caldavCalendar = CaldavCalendar(
            uuid = UUIDHelper.newUUID(),
            account = caldavAccount.uuid,
            url = url,
            name = newName,
            color = selectedColor,
            icon = selectedIcon.value,
        )
        caldavDao.insert(caldavCalendar)
        setResult(
                Activity.RESULT_OK,
                Intent().putExtra(MainActivity.OPEN_FILTER, CaldavFilter(caldavCalendar)))
        finish()
    }

    protected suspend fun updateCalendar() {
        val result = caldavCalendar!!.copy(
            name = newName,
            color = selectedColor,
            icon = selectedIcon.value,
        )
        caldavDao.update(result)
        setResult(
                Activity.RESULT_OK,
                Intent(TaskListFragment.ACTION_RELOAD)
                        .putExtra(MainActivity.OPEN_FILTER, CaldavFilter(result)))
        finish()
    }

    override fun hasChanges(): Boolean =
            if (caldavCalendar == null)
                !isNullOrEmpty(newName) || selectedColor != 0 || selectedIcon.value?.isBlank() == false
            else
                nameChanged() || iconChanged() || colorChanged()

    private fun nameChanged(): Boolean = caldavCalendar!!.name != newName

    private fun colorChanged(): Boolean = selectedColor != caldavCalendar!!.color

    private fun iconChanged(): Boolean = selectedIcon.value != (caldavCalendar!!.icon ?: TasksIcons.LIST)

    private val newName: String
        get() = textState.value.trim { it <= ' '}

    override fun finish() {
        // hideKeyboard(name)
        super.finish()
    }

    override fun discard() {
        if (!requestInProgress()) {
            super.discard()
        }
    }

    override fun promptDelete() {
        if (!requestInProgress()) {
            super.promptDelete()
        }
    }

    override suspend fun delete() {
        showProgressIndicator()
        deleteCalendar(caldavAccount, caldavCalendar!!)
    }

    protected suspend fun onDeleted(deleted: Boolean) {
        if (deleted) {
            taskDeleter.delete(caldavCalendar!!)
            setResult(Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED))
            finish()
        }
    }

    @Composable
    fun BaseCaldavSettingsContent (
        optionButton: @Composable () -> Unit = { if (!isNew) DeleteButton(caldavCalendar?.name ?: "") { delete() } },
        fab: @Composable () -> Unit = {},
        extensionContent: @Composable ColumnScope.() -> Unit = {},
    ) {
        BaseSettingsContent (
            optionButton = optionButton,
            extensionContent = extensionContent,
            fab = fab,
        )
        Toaster(state = snackbar)
    }

    companion object {
        const val EXTRA_CALDAV_CALENDAR = "extra_caldav_calendar"
        const val EXTRA_CALDAV_ACCOUNT = "extra_caldav_account"
    }
}