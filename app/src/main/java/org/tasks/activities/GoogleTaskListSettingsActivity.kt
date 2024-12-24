package org.tasks.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.lifecycleScope
import com.google.api.services.tasks.model.TaskList
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.compose.settings.Toaster
import org.tasks.data.dao.GoogleTaskListDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.filters.Filter
import org.tasks.filters.GtasksFilter
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GoogleTaskListSettingsActivity : BaseListSettingsActivity() {
    @Inject lateinit var googleTaskListDao: GoogleTaskListDao
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private var isNewList = false
    private lateinit var gtasksList: CaldavCalendar
    private val createListViewModel: CreateListViewModel by viewModels()
    private val renameListViewModel: RenameListViewModel by viewModels()
    private val deleteListViewModel: DeleteListViewModel by viewModels()
    override val defaultIcon = TasksIcons.LIST

    val snackbar = SnackbarHostState()

    override fun onCreate(savedInstanceState: Bundle?) {
        gtasksList = intent.getParcelableExtra(EXTRA_STORE_DATA)
                ?: CaldavCalendar(
                    account = intent.getParcelableExtra<CaldavAccount>(EXTRA_ACCOUNT)!!.username
                ).apply {
                    isNewList = true
                }
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            baseViewModel.setColor(gtasksList.color)
            baseViewModel.setIcon(gtasksList.icon ?: defaultIcon)
        }

        if (!isNewList) baseViewModel.setTitle(gtasksList.name!!)

        if (createListViewModel.inProgress
                || renameListViewModel.inProgress
                || deleteListViewModel.inProgress) {
            showProgressIndicator()
        }

        createListViewModel.observe(this, this::onListCreated, this::requestFailed)
        renameListViewModel.observe(this, this::onListRenamed, this::requestFailed)
        deleteListViewModel.observe(this, this::onListDeleted, this::requestFailed)

        setContent {
            TasksTheme {
                BaseSettingsContent()
                Toaster(state = snackbar)
            }
        }

        updateTheme()
    }

    override val filter: Filter?
        get() = if (isNewList) null else GtasksFilter(gtasksList)

    override val toolbarTitle: String
        get() = if (isNew) getString(R.string.new_list) else gtasksList.name!!

    private fun showProgressIndicator() {
        baseViewModel.showProgress(true)
    }

    private fun hideProgressIndicator() {
        baseViewModel.showProgress(false)
    }

    private fun requestInProgress() = baseViewModel.viewState.value.showProgress

    override suspend fun save() {
        if (requestInProgress()) {
            return
        }
        val newName = newName
        if (isNullOrEmpty(newName)) {
            baseViewModel.setError(getString(R.string.name_cannot_be_empty))
            return
        }
        when {
            isNewList -> {
                showProgressIndicator()
                createListViewModel.createList(gtasksList.account!!, newName)
            }
            nameChanged() -> {
                showProgressIndicator()
                renameListViewModel.renameList(gtasksList, newName)
            }
            else -> {
                if (colorChanged() || iconChanged()) {
                    gtasksList.color = baseViewModel.color
                    googleTaskListDao.insertOrReplace(
                        gtasksList.copy(
                            icon = baseViewModel.icon
                        )
                    )
                    localBroadcastManager.broadcastRefresh()
                    setResult(
                            Activity.RESULT_OK,
                            Intent(TaskListFragment.ACTION_RELOAD)
                                    .putExtra(MainActivity.OPEN_FILTER, GtasksFilter(gtasksList)))
                }
                finish()
            }
        }
    }

    override fun promptDelete() {
        if (!requestInProgress()) {
            super.promptDelete()
        }
    }

    override suspend fun delete() {
        showProgressIndicator()
        deleteListViewModel.deleteList(gtasksList)
    }

    override fun discard() {
        if (!requestInProgress()) {
            super.discard()
        }
    }

    private val newName: String
        get() = baseViewModel.title.trim { it <= ' ' }

    override fun hasChanges(): Boolean =
        if (isNewList) {
            baseViewModel.color != 0 || !isNullOrEmpty(newName)
        } else colorChanged() || nameChanged() || iconChanged()

    private fun colorChanged() = baseViewModel.color != gtasksList.color

    private fun iconChanged() = baseViewModel.icon != (gtasksList.icon ?: TasksIcons.LIST)

    private fun nameChanged() = newName != gtasksList.name

    private suspend fun onListCreated(taskList: TaskList) {
        val result = gtasksList.copy(
            uuid = taskList.id,
            name = taskList.title,
            color = baseViewModel.color,
            icon = baseViewModel.icon,
        )
        val id = googleTaskListDao.insertOrReplace(result)

        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(MainActivity.OPEN_FILTER, GtasksFilter(result.copy(id = id))))
        finish()
    }

    private fun onListDeleted(deleted: Boolean) {
        if (deleted) {
            lifecycleScope.launch {
                withContext(NonCancellable) {
                    taskDeleter.delete(gtasksList)
                }
                setResult(Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED))
                finish()
            }
        }
    }

    private suspend fun onListRenamed(taskList: TaskList) {
        val result = gtasksList.copy(
            name = taskList.title,
            color = baseViewModel.color,
            icon = baseViewModel.icon,
        )
        googleTaskListDao.insertOrReplace(result)

        setResult(
                Activity.RESULT_OK,
                Intent(TaskListFragment.ACTION_RELOAD)
                    .putExtra(MainActivity.OPEN_FILTER, GtasksFilter(result)))
        finish()
    }

    private fun requestFailed(error: Throwable) {
        Timber.e(error)
        hideProgressIndicator()
        lifecycleScope.launch { snackbar.showSnackbar(getString(R.string.gtasks_GLA_errorIOAuth)) }
        //toast(R.string.gtasks_GLA_errorIOAuth)
        return
    }

    companion object {
        const val EXTRA_ACCOUNT = "extra_account"
        const val EXTRA_STORE_DATA = "extra_store_data"
    }
}