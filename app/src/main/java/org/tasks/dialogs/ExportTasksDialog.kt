package org.tasks.dialogs

import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.backup.TasksJsonExporter
import javax.inject.Inject

@AndroidEntryPoint
class ExportTasksDialog : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var tasksJsonExporter: TasksJsonExporter

    private var encrypt = false
    private var password = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogBuilder
            .newDialog(R.string.backup_BAc_export)
            .setContent {
                var encryptState by remember { mutableStateOf(encrypt) }
                var passwordState by remember { mutableStateOf(password) }
                Column(
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = encryptState,
                            onCheckedChange = {
                                encryptState = it
                                encrypt = it
                            }
                        )
                        Text(
                            text = stringResource(R.string.encrypt_backup),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (encryptState) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passwordState,
                            onValueChange = {
                                passwordState = it
                                password = it
                            },
                            label = { Text(stringResource(R.string.password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
            .setPositiveButton(R.string.ok) { _, _ ->
                val progressDialog = dialogBuilder.newProgressDialog().apply {
                    setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                    progress = 0
                    setCancelable(false)
                    isIndeterminate = false
                }
                progressDialog.show()
                lifecycleScope.launch(NonCancellable) {
                    tasksJsonExporter.exportTasks(
                        activity,
                        TasksJsonExporter.ExportType.EXPORT_TYPE_MANUAL,
                        progressDialog,
                        if (encrypt) password else null
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle.EMPTY)
    }

    companion object {
        const val REQUEST_KEY = "export_tasks_result"

        fun newExportTasksDialog() = ExportTasksDialog()
    }
}
