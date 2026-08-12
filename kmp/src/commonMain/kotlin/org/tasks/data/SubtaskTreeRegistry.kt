package org.tasks.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class SubtaskTreeRegistry {
    private val trees = MutableStateFlow<List<SubtaskTrees>>(emptyList())

    val openTrees: List<SubtaskTrees> get() = trees.value

    fun open(): SubtaskTrees {
        val tree = SubtaskTrees()
        trees.update { it.plus(tree) }
        return tree
    }

    fun close(tree: SubtaskTrees) {
        trees.update { it.minus(tree) }
    }

    fun holding(key: String): SubtaskTrees? = trees.value.firstOrNull { it.get(key) != null }

    fun holds(id: Long, remoteId: String?): Boolean = trees.value.any { it.holds(id, remoteId) }

    fun isDoomed(id: Long, remoteId: String?): Boolean =
        trees.value.any { it.isDoomed(id, remoteId) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val deletions: Flow<Map<String, Boolean>> =
        trees
            .flatMapLatest { open ->
                when {
                    open.isEmpty() -> flowOf(emptyMap())
                    else -> combine(open.map { it.nodes }) { maps ->
                        maps.fold(emptyMap<String, Boolean>()) { all, nodes ->
                            all.plus(nodes.deletions())
                        }
                    }
                }
            }
            .distinctUntilChanged()
}
