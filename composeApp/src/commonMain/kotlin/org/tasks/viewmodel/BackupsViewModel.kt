@file:Suppress("TooGenericExceptionCaught")
package org.tasks.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.tasks.backup.shared.TasksJsonExporter
import org.tasks.time.DateTimeUtils2
import java.io.File
import java.io.FileOutputStream

sealed interface ExportState {
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Done(val path: String, val count: Int) : ExportState
    data class Error(val message: String) : ExportState
}

class BackupsViewModel(
    private val exporter: TasksJsonExporter,
) : ViewModel() {

    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set

    fun export(outputDir: String) {
        if (exportState is ExportState.Exporting) return
        exportState = ExportState.Exporting
        viewModelScope.launch {
            try {
                val dir = File(outputDir)
                dir.mkdirs()
                val filename = "user.${newDateTimeString()}.json"
                val file = File(dir, filename)
                FileOutputStream(file).use { os ->
                    exporter.exportTasks(os)
                }
                exportState = ExportState.Done(
                    path = file.absolutePath,
                    count = exporter.exportCount,
                )
            } catch (e: Exception) {
                exportState = ExportState.Error(
                    message = e.message ?: "Unknown error",
                )
            }
        }
    }

    fun resetState() {
        exportState = ExportState.Idle
    }

    private fun newDateTimeString(): String {
        val now = DateTimeUtils2.currentTimeMillis()
        val d = org.tasks.time.DateTime(now)
        return d.toString("yyyyMMdd'T'HHmm")
    }
}
