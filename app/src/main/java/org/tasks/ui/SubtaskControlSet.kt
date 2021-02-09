package org.tasks.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.OnClick
import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Join
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.ui.CheckableImageView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.data.*
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.locale.Locale
import org.tasks.tasklist.SubtaskViewHolder
import org.tasks.tasklist.SubtasksRecyclerAdapter
import javax.inject.Inject

@AndroidEntryPoint
class SubtaskControlSet : TaskEditControlFragment(), SubtaskViewHolder.Callbacks {
    @BindView(R.id.recycler_view)
    lateinit var recyclerView: RecyclerView

    @BindView(R.id.new_subtasks)
    lateinit var newSubtaskContainer: LinearLayout

    @Inject lateinit var activity: Activity
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var toaster: Toaster
    @Inject lateinit var taskCreator: TaskCreator
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var locale: Locale
    @Inject lateinit var checkBoxProvider: CheckBoxProvider
    @Inject lateinit var chipProvider: ChipProvider
    
    private val listViewModel: TaskListViewModel by viewModels()
    private val refreshReceiver = RefreshReceiver()
    private var remoteList: Filter? = null
    private var googleTask: GoogleTask? = null
    private lateinit var recyclerAdapter: SubtasksRecyclerAdapter
    
    override fun createView(savedInstanceState: Bundle?) {
        viewModel.newSubtasks.forEach { addSubtask(it) }
        recyclerAdapter = SubtasksRecyclerAdapter(activity, chipProvider, checkBoxProvider, this)
        viewModel.task?.let {
            if (it.id > 0) {
                recyclerAdapter.submitList(listViewModel.value)
                listViewModel.setFilter(Filter("subtasks", getQueryTemplate(it)))
                (recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
                recyclerView.layoutManager = LinearLayoutManager(activity)
                recyclerView.isNestedScrollingEnabled = false
                listViewModel.observe(this) {
                    list: List<TaskContainer?>? -> recyclerAdapter.submitList(list)
                }
                recyclerView.adapter = recyclerAdapter
            }
        }
    }

    override val layout = R.layout.control_set_subtasks

    override val icon = R.drawable.ic_subdirectory_arrow_right_black_24dp

    override fun controlId() = TAG

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerRefreshReceiver(refreshReceiver)
        lifecycleScope.launch {
            viewModel.task?.let {
                googleTask = googleTaskDao.getByTaskId(it.id)
                updateUI()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    @OnClick(R.id.add_subtask)
    fun addSubtask() {
        if (isGoogleTaskChild) {
            toaster.longToast(R.string.subtasks_multilevel_google_task)
        } else {
            lifecycleScope.launch {
                val task = taskCreator.createWithValues("")
                viewModel.newSubtasks.add(task)
                val editText = addSubtask(task)
                editText.requestFocus()
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun addSubtask(task: Task): EditText {
        val view = LayoutInflater.from(activity)
                .inflate(R.layout.editable_subtask_adapter_row_body, newSubtaskContainer, false) as ViewGroup
        view.findViewById<View>(R.id.clear).setOnClickListener { newSubtaskContainer.removeView(view) }
        val editText = view.getChildAt(2) as EditText
        editText.setText(task.title)
        editText.setHorizontallyScrolling(false)
        editText.setLines(1)
        editText.maxLines = Int.MAX_VALUE
        editText.isFocusable = true
        editText.isEnabled = true
        editText.addTextChangedListener { text: Editable? ->
            task.title = text?.toString()
        }
        editText.setOnEditorActionListener { _, actionId: Int, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                if (editText.text.isNotEmpty()) {
                    addSubtask()
                }
                return@setOnEditorActionListener true
            }
            false
        }
        val completeBox: CheckableImageView = view.findViewById(R.id.completeBox)
        completeBox.isChecked = task.isCompleted
        updateCompleteBox(task, completeBox, editText)
        completeBox.setOnClickListener { updateCompleteBox(task, completeBox, editText) }
        newSubtaskContainer.addView(view)
        return editText
    }

    private fun updateCompleteBox(task: Task, completeBox: CheckableImageView, editText: EditText) {
        val isComplete = completeBox.isChecked
        task.completionDate = if (isComplete) now() else 0
        completeBox.setImageDrawable(
                checkBoxProvider.getCheckBox(isComplete, false, task.priority))
        editText.paintFlags = if (isComplete) {
            editText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            editText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    private val isGoogleTaskChild: Boolean
        get() = (remoteList is GtasksFilter
                && googleTask != null && googleTask!!.parent > 0 && googleTask!!.listId == (remoteList as GtasksFilter).remoteId)

    private fun updateUI() {
        if (isGoogleTaskChild) {
            recyclerView.visibility = View.GONE
            newSubtaskContainer.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            newSubtaskContainer.visibility = View.VISIBLE
            recyclerAdapter.setMultiLevelSubtasksEnabled(remoteList !is GtasksFilter)
            refresh()
        }
    }

    fun onRemoteListChanged(filter: Filter?) {
        remoteList = filter
        updateUI()
    }

    private fun refresh() {
        listViewModel.invalidate()
    }

    override fun openSubtask(task: Task) {
        (activity as MainActivity).taskListFragment?.onTaskListItemClicked(task)
    }

    override fun toggleSubtask(taskId: Long, collapsed: Boolean) {
        lifecycleScope.launch {
            taskDao.setCollapsed(taskId, collapsed)
            localBroadcastManager.broadcastRefresh()
        }
    }

    override fun complete(task: Task, completed: Boolean) {
        lifecycleScope.launch {
            taskCompleter.setComplete(task, completed)
        }
    }

    private inner class RefreshReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_subtask_pref
        private fun getQueryTemplate(task: Task): QueryTemplate = QueryTemplate()
                .join(
                        Join.left(
                                GoogleTask.TABLE,
                                Criterion.and(
                                        GoogleTask.PARENT.eq(task.id),
                                        GoogleTask.TASK.eq(Task.ID),
                                        GoogleTask.DELETED.eq(0))))
                .where(
                        Criterion.and(
                                activeAndVisible(),
                                Criterion.or(Task.PARENT.eq(task.id), GoogleTask.TASK.gt(0))))
    }
}