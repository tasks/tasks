package org.tasks.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import com.google.android.material.chip.ChipGroup
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.gtasks.GtasksListService
import com.todoroo.astrid.service.TaskMover
import org.tasks.R
import org.tasks.activities.ListPicker
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.data.GoogleTask
import org.tasks.data.GoogleTaskDao
import org.tasks.injection.FragmentComponent
import org.tasks.preferences.DefaultFilterProvider
import javax.inject.Inject

class ListFragment : TaskEditControlFragment() {
    @BindView(R.id.chip_group)
    lateinit var chipGroup: ChipGroup

    @Inject lateinit var gtasksListService: GtasksListService
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var taskMover: TaskMover
    @Inject lateinit var chipProvider: ChipProvider
    
    private var originalList: Filter? = null
    private lateinit var selectedList: Filter
    private lateinit var callback: OnListChanged

    interface OnListChanged {
        fun onListChanged(filter: Filter?)
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        callback = activity as OnListChanged
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        if (savedInstanceState != null) {
            originalList = savedInstanceState.getParcelable(EXTRA_ORIGINAL_LIST)
            setSelected(savedInstanceState.getParcelable(EXTRA_SELECTED_LIST)!!)
        } else {
            if (task.isNew) {
                if (task.hasTransitory(GoogleTask.KEY)) {
                    val listId = task.getTransitory<String>(GoogleTask.KEY)!!
                    val googleTaskList = gtasksListService.getList(listId)
                    if (googleTaskList != null) {
                        originalList = GtasksFilter(googleTaskList)
                    }
                } else if (task.hasTransitory(CaldavTask.KEY)) {
                    val caldav = caldavDao.getCalendarByUuid(task.getTransitory(CaldavTask.KEY)!!)
                    if (caldav != null) {
                        originalList = CaldavFilter(caldav)
                    }
                }
            } else {
                val googleTask = googleTaskDao.getByTaskId(task.id)
                val caldavTask = caldavDao.getTask(task.id)
                if (googleTask != null) {
                    val googleTaskList = gtasksListService.getList(googleTask.listId)
                    if (googleTaskList != null) {
                        originalList = GtasksFilter(googleTaskList)
                    }
                } else if (caldavTask != null) {
                    val calendarByUuid = caldavDao.getCalendarByUuid(caldavTask.calendar!!)
                    if (calendarByUuid != null) {
                        originalList = CaldavFilter(calendarByUuid)
                    }
                }
            }
            setSelected(originalList ?: defaultFilterProvider.defaultList)
        }
        return view
    }

    private fun setSelected(filter: Filter) {
        selectedList = filter
        refreshView()
        callback.onListChanged(filter)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_ORIGINAL_LIST, originalList)
        outState.putParcelable(EXTRA_SELECTED_LIST, selectedList)
    }

    override val layout: Int
        get() = R.layout.control_set_remote_list

    override val icon: Int
        get() = R.drawable.ic_list_24px

    override fun controlId() = TAG

    override fun onRowClick() = openPicker()

    override val isClickable: Boolean
        get() = true

    private fun openPicker() =
            ListPicker.newListPicker(selectedList, this, REQUEST_CODE_SELECT_LIST)
                    .show(parentFragmentManager, FRAG_TAG_GOOGLE_TASK_LIST_SELECTION)

    override fun requiresId() = true

    override fun apply(task: Task) {
        if (isNew || hasChanges()) {
            task.parent = 0
            taskMover.move(listOf(task.id), selectedList)
        }
    }

    override fun hasChanges(original: Task) = hasChanges()

    private fun hasChanges() = selectedList != originalList

    override fun inject(component: FragmentComponent) = component.inject(this)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SELECT_LIST) {
            if (resultCode == Activity.RESULT_OK) {
                data?.getParcelableExtra<Filter>(ListPicker.EXTRA_SELECTED_FILTER)?.let {
                    setList(it)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setList(list: Filter) {
        if (list is GtasksFilter || list is CaldavFilter) {
            setSelected(list)
        } else {
            throw RuntimeException("Unhandled filter type")
        }
    }

    private fun refreshView() {
        chipGroup.removeAllViews()
        val chip = chipProvider.newChip(selectedList, R.drawable.ic_list_24px, showText = true, showIcon = true)!!
        chip.setOnClickListener { openPicker() }
        chipGroup.addView(chip)
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_google_task_list
        private const val FRAG_TAG_GOOGLE_TASK_LIST_SELECTION = "frag_tag_google_task_list_selection"
        private const val EXTRA_ORIGINAL_LIST = "extra_original_list"
        private const val EXTRA_SELECTED_LIST = "extra_selected_list"
        private const val REQUEST_CODE_SELECT_LIST = 10101
    }
}