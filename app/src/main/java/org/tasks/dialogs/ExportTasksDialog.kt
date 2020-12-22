package org.tasks.dialogs

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.tasks.backup.TasksJsonExporter
import javax.inject.Inject

@AndroidEntryPoint
class ExportTasksDialog : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var tasksJsonExporter: TasksJsonExporter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val progressDialog = dialogBuilder.newProgressDialog().apply {
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progress = 0
            setCancelable(false)
            isIndeterminate = false
        }

        progressDialog.show()
        isCancelable = false
        lifecycleScope.launch(NonCancellable) {
            tasksJsonExporter.exportTasks(
                    activity, TasksJsonExporter.ExportType.EXPORT_TYPE_MANUAL, progressDialog)
        }

        return progressDialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, null)
    }

    companion object {
        fun newExportTasksDialog(target: Fragment, rc: Int) = ExportTasksDialog().apply {
            setTargetFragment(target, rc)
        }
    }
}