package org.tasks.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.edit.Priority
import org.tasks.databinding.DialogDateTimePickerBinding
import org.tasks.databinding.DialogPriorityPickerBinding
import javax.inject.Inject

@AndroidEntryPoint
class PriorityPicker : DialogFragment() {

    @Inject lateinit var taskDao: TaskDao

    private val taskIds: LongArray
        get() = arguments?.getLongArray(EXTRA_TASKS) ?: longArrayOf()

    companion object {
        const val EXTRA_TASKS = "extra_tasks"
        const val EXTRA_DESATURATE = "extra_desaturatee"

        fun newPriorityPicker(desaturateColors: Boolean, tasks: List<Task>): PriorityPicker {
            val bundle = Bundle()
            bundle.putLongArray(EXTRA_TASKS, tasks.map { it.id }.toLongArray())
            bundle.putBoolean(EXTRA_DESATURATE, desaturateColors)
            val fragment = PriorityPicker()
            fragment.arguments = bundle
            return fragment
        }
    }

    lateinit var binding: DialogDateTimePickerBinding
    private val priorityPickerViewModel: PriorityPickerViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return requireActivity().let { fragmentActivity ->
            val inflater = fragmentActivity.layoutInflater
            val binding = DialogPriorityPickerBinding.inflate(inflater, null, false)
            binding.priorityRow.setContent { Priority(selected = priorityPickerViewModel.priority.collectAsStateLifecycleAware().value,
                onClick = { priorityPickerViewModel.setPriority( it ) }, desaturate = savedInstanceState?.getBoolean(
                EXTRA_DESATURATE) ?: false) }
            val builder = AlertDialog.Builder(fragmentActivity)
                .setTitle(R.string.change_priority)
                .setView(binding.root)

            builder.setNegativeButton(R.string.cancel) { _, _ ->

            }
            builder.setPositiveButton(R.string.ok) { _, _ ->
                changePriority()
            }
            builder.create()
        }
    }

    private fun changePriority() {
        lifecycleScope.launch(NonCancellable) {
            taskDao
                .fetch(taskIds.toList())
                .forEach {
                    taskDao.save(it.copy(priority = priorityPickerViewModel.priority.value))
                }
        }
        dismiss()
    }

}
