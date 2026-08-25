package org.tasks.preferences.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.tasks.ai.AiCredentialStore
import org.tasks.ai.AiGate
import org.tasks.ai.ModelInfo
import org.tasks.ai.OpenRouterClientProvider
import org.tasks.compose.settings.AiModelOption
import org.tasks.compose.settings.AiTaskCreationState
import org.tasks.http.UnauthorizedException
import org.tasks.preferences.TasksPreferences
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.ai_key_invalid
import tasks.kmp.generated.resources.ai_no_compatible_models
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AiTaskCreationViewModel @Inject constructor(
    private val gate: AiGate,
    private val credentials: AiCredentialStore,
    private val tasksPreferences: TasksPreferences,
    private val clientProvider: OpenRouterClientProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(AiTaskCreationState())
    val state: StateFlow<AiTaskCreationState> = _state.asStateFlow()

    private val _showDisclosure = MutableStateFlow(false)
    val showDisclosure: StateFlow<Boolean> = _showDisclosure.asStateFlow()

    /** Non-blocking message (e.g. no compatible models), consumed by the fragment as a toast. */
    private val _message = MutableStateFlow<StringResource?>(null)
    val message: StateFlow<StringResource?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update {
            it.copy(
                enabled = gate.isEnabled(),
                hasStoredKey = credentials.hasApiKey(),
                selectedModel = tasksPreferences
                    .get(TasksPreferences.aiModel, "")
                    .takeIf { model -> model.isNotBlank() },
            )
        }
    }

    fun onKeyChange(key: String) {
        _state.update { it.copy(keyInput = key, keyError = null) }
    }

    /**
     * Verifies [AiTaskCreationState.keyInput] against OpenRouter before storing it. Nothing is
     * written when the key is rejected, so [AiGate.canCall] cannot be satisfied by a bad key.
     */
    fun onKeySubmit() = viewModelScope.launch {
        val key = _state.value.keyInput.trim()
        if (key.isBlank()) return@launch
        _state.update { it.copy(loadingModels = true, keyError = null) }
        val models = try {
            clientProvider.validateKey(key)
        } catch (e: UnauthorizedException) {
            Timber.d(e)
            _state.update {
                it.copy(loadingModels = false, keyError = Res.string.ai_key_invalid)
            }
            return@launch
        } catch (e: Exception) {
            Timber.e(e)
            _state.update {
                it.copy(loadingModels = false, keyError = Res.string.ai_key_invalid)
            }
            return@launch
        }

        val compatible = models
            .filter { it.supportsStructuredOutputs }
            .sortedWith(compareByDescending<ModelInfo> { it.isFree }.thenBy { it.id })

        if (compatible.isEmpty()) {
            _message.value = Res.string.ai_no_compatible_models
            _state.update { it.copy(loadingModels = false) }
            return@launch
        }

        credentials.setApiKey(key)

        val selected = _state.value.selectedModel
            ?.takeIf { current -> compatible.any { it.id == current } }
            ?: compatible.firstOrNull { it.isFree }?.id
            ?: compatible.first().id
        tasksPreferences.set(TasksPreferences.aiModel, selected)

        _state.update {
            it.copy(
                loadingModels = false,
                keyInput = "",
                hasStoredKey = true,
                models = compatible.map { model ->
                    AiModelOption(id = model.id, label = model.name ?: model.id)
                },
                selectedModel = selected,
            )
        }
    }

    /**
     * Toggling on while unconsented raises the disclosure instead of enabling, so consent
     * cannot be bypassed from this screen.
     */
    fun onEnabledChange(enabled: Boolean) = viewModelScope.launch {
        if (!enabled) {
            tasksPreferences.set(TasksPreferences.aiTaskCreationEnabled, false)
            _state.update { it.copy(enabled = false) }
            return@launch
        }
        if (!gate.hasConsent()) {
            _showDisclosure.value = true
            return@launch
        }
        tasksPreferences.set(TasksPreferences.aiTaskCreationEnabled, true)
        _state.update { it.copy(enabled = true) }
    }

    fun onDisclosureAccepted() = viewModelScope.launch {
        gate.acceptConsent()
        tasksPreferences.set(TasksPreferences.aiTaskCreationEnabled, true)
        _showDisclosure.value = false
        _state.update { it.copy(enabled = true) }
    }

    fun onDisclosureDismissed() {
        _showDisclosure.value = false
    }

    fun onModelSelected(id: String) = viewModelScope.launch {
        tasksPreferences.set(TasksPreferences.aiModel, id)
        _state.update { it.copy(selectedModel = id) }
    }

    fun messageShown() {
        _message.value = null
    }
}
