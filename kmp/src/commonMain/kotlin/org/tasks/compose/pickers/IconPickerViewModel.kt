package org.tasks.compose.pickers

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.tasks.compose.components.iconExists
import tasks.kmp.generated.resources.Res


@OptIn(ExperimentalResourceApi::class, FlowPreview::class, ExperimentalCoroutinesApi::class)
class IconPickerViewModel : ViewModel() {
    @Stable
    data class ViewState(
        val icons: ImmutableMap<String, ImmutableList<Icon>> = persistentMapOf(),
        val query: String = "",
        val collapsed: ImmutableSet<String> = persistentSetOf(),
    )

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState>
        get() = _viewState.asStateFlow()

    private val _searchResults = MutableStateFlow(persistentListOf<Icon>())
    val searchResults: StateFlow<ImmutableList<Icon>>
        get() = _searchResults.asStateFlow()

    init {
        viewModelScope.launch {
            val metadata: IconMetadata = withContext(Dispatchers.Default) {
                Json { ignoreUnknownKeys = true }.decodeFromString(Res.readBytes("files/icons.json").decodeToString())
            }
            val map = mutableMapOf<String, ArrayList<Icon>>()
            metadata.icons
                .filter { it.imageExists }
                .forEach { icon ->
                    icon.categories.forEach { category ->
                        map.getOrPut(category) { ArrayList() }.add(icon)
                    }
                }

            _viewState.update { state ->
                state.copy(
                    icons = map.entries.sortedBy { it.key }.associate { (k, v) -> k to v.toPersistentList() }.toPersistentMap(),
                )
            }

            _viewState
                .map { it.query }
                .debounce(333)
                .flowOn(Dispatchers.Default)
                .mapLatest { query ->
                    metadata
                        .icons
                        .filter { icon ->
                            icon.name.contains(query, ignoreCase = true) ||
                                    icon.tags.any { it.contains(query, ignoreCase = true) }
                        }
                }
                .onEach {
                    _searchResults.value = it.toPersistentList()
                }
                .launchIn(viewModelScope)
        }
    }

    fun onQueryChange(query: String) {
        _viewState.update { state ->
            state.copy(query = query)
        }
    }

    fun setCollapsed(category: String, collapsed: Boolean) {
        _viewState.update { state ->
            state.copy(
                collapsed = if (collapsed) {
                    state.collapsed + category
                } else {
                    state.collapsed - category
                }.toPersistentSet()
            )
        }
    }
}

@Serializable
@Stable
data class IconMetadata(
    val icons: List<Icon>
)

@Serializable
@Stable
data class Icon(
    val name: String,
    val categories: List<String>,
    val tags: List<String>,
) {
    val imageExists: Boolean
        get() = iconExists(name)
}


fun String.uppercaseFirstLetter(): String {
    return replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }
}
