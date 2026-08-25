package org.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.tasks.ai.AiFailure
import org.tasks.ai.AiParseResult
import org.tasks.ai.AiTaskParser
import org.tasks.ai.ParsedTask
import org.tasks.ai.applyTo
import org.tasks.data.GoogleTask
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Tag
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.ai_error_bad_response
import tasks.kmp.generated.resources.ai_error_network
import tasks.kmp.generated.resources.ai_error_no_tasks
import tasks.kmp.generated.resources.ai_error_rate_limited
import tasks.kmp.generated.resources.ai_error_unauthorized
import tasks.kmp.generated.resources.ai_error_unavailable
import tasks.kmp.generated.resources.ai_not_configured
import javax.inject.Inject

sealed interface AiCaptureState {
    data object Input : AiCaptureState
    data object Loading : AiCaptureState
    data class Review(val items: List<ReviewItem>) : AiCaptureState
    data class Error(val messageRes: StringResource) : AiCaptureState
}

/** One parsed task awaiting confirmation. [task] is built but deliberately unsaved. */
data class ReviewItem(
    val task: Task,
    val parsed: ParsedTask,
    val checked: Boolean = true,
    val chips: List<String> = emptyList(),
)

@HiltViewModel
class AiCaptureViewModel @Inject constructor(
    private val parser: AiTaskParser,
    private val taskCreator: TaskCreator,
) : ViewModel() {

    private val _state = MutableStateFlow<AiCaptureState>(AiCaptureState.Input)
    val state: StateFlow<AiCaptureState> = _state.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private var parseJob: Job? = null

    fun onInputChange(text: String) {
        _input.value = text
    }

    fun reset() {
        parseJob?.cancel()
        parseJob = null
        _input.value = ""
        _state.value = AiCaptureState.Input
    }

    fun submit(text: String = _input.value) {
        parseJob?.cancel()
        _input.value = text
        parseJob = viewModelScope.launch {
            _state.value = AiCaptureState.Loading
            when (val result = parser.parse(text)) {
                is AiParseResult.NotConfigured ->
                    _state.value = AiCaptureState.Error(Res.string.ai_not_configured)

                is AiParseResult.Failure ->
                    _state.value = AiCaptureState.Error(result.reason.messageRes())

                is AiParseResult.Success -> {
                    val lists = parser.writableLists()
                    val knownTags = parser.knownTags()
                    val items = result.tasks.map { parsed ->
                        // createWithValues still runs TitleParser on the AI-produced title. That
                        // is harmless: the prompt strips scheduling language from the title, and
                        // applyTo runs afterward and wins on any field the model did set.
                        val task = taskCreator.createWithValues(null, parsed.title)
                        parsed.applyTo(task, lists, knownTags)
                        ReviewItem(
                            task = task,
                            parsed = parsed,
                            chips = chipsFor(task, parsed, lists),
                        )
                    }
                    _state.value = AiCaptureState.Review(items)
                }
            }
        }
    }

    fun toggle(index: Int) {
        val current = _state.value as? AiCaptureState.Review ?: return
        _state.value = AiCaptureState.Review(
            current.items.mapIndexed { i, item ->
                if (i == index) item.copy(checked = !item.checked) else item
            }
        )
    }

    fun openInEditor(index: Int): Task? =
        (_state.value as? AiCaptureState.Review)?.items?.getOrNull(index)?.task

    /** Persists only the checked items. Nothing reaches the database before this runs. */
    suspend fun confirm(): List<Task> {
        val current = _state.value as? AiCaptureState.Review ?: return emptyList()
        return current.items
            .filter { it.checked }
            .map { taskCreator.persistNewTask(it.task) }
    }

    private fun AiFailure.messageRes(): StringResource = when (this) {
        AiFailure.UNAUTHORIZED -> Res.string.ai_error_unauthorized
        AiFailure.RATE_LIMITED -> Res.string.ai_error_rate_limited
        AiFailure.UNAVAILABLE -> Res.string.ai_error_unavailable
        AiFailure.BAD_RESPONSE -> Res.string.ai_error_bad_response
        AiFailure.NO_TASKS -> Res.string.ai_error_no_tasks
        AiFailure.NETWORK -> Res.string.ai_error_network
    }
}

/**
 * Chips describe what was actually applied to [task], not what the model asked for: an
 * unmatched list or tag is dropped by [applyTo], and showing it here would mislead.
 */
private fun chipsFor(
    task: Task,
    parsed: ParsedTask,
    lists: List<CaldavFilter>,
): List<String> = buildList {
    val listUuid = task.getTransitory<String>(CaldavTask.KEY)
        ?: task.getTransitory<String>(GoogleTask.KEY)
    listUuid?.let { uuid -> lists.firstOrNull { it.uuid == uuid }?.let { add(it.title) } }

    if (task.hasDueDate()) {
        parsed.due?.takeIf { it.isNotBlank() }?.let { add(it.replace('T', ' ')) }
    }

    when (task.priority) {
        Task.Priority.HIGH -> add("!!!")
        Task.Priority.MEDIUM -> add("!!")
        Task.Priority.LOW -> add("!")
        else -> {}
    }

    task.getTransitory<ArrayList<String>>(Tag.KEY)?.let { addAll(it) }
}
