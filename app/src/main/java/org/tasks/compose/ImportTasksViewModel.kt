package org.tasks.compose

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.backup.TasksJsonImporter
import javax.inject.Inject

@HiltViewModel
class ImportTasksViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val importer: TasksJsonImporter,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    val importUri: StateFlow<Uri?> = savedStateHandle
        .getStateFlow<String?>(KEY_IMPORT_URI, null)
        .map { it?.toUri() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setImportUri(uri: Uri?) {
        savedStateHandle[KEY_IMPORT_URI] = uri?.toString()
    }

    fun startImport(uri: Uri) {
        if (_state.value is ImportState.Importing) return // Already importing

        _state.value = ImportState.Importing("")
        viewModelScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                try {
                    val result = importer.importTasks(context, uri) { message ->
                        _state.value = ImportState.Importing(message)
                    }
                    _state.value = ImportState.Complete(result)
                } catch (e: Exception) {
                    _state.value = ImportState.Error
                }
            }
        }
    }

    fun reset() {
        savedStateHandle[KEY_IMPORT_URI] = null
        _state.value = ImportState.Idle
    }

    sealed class ImportState {
        data object Idle : ImportState()
        data class Importing(val message: String) : ImportState()
        data class Complete(val result: TasksJsonImporter.ImportResult) : ImportState()
        data object Error : ImportState()
    }

    companion object {
        private const val KEY_IMPORT_URI = "import_uri"
    }
}
