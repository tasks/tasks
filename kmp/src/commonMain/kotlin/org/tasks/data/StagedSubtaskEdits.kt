package org.tasks.data

internal class StagedSubtaskEdits(
    private val trees: SubtaskTrees,
    private val rootKey: () -> String,
) {
    private val added = mutableSetOf<String>()
    private val edited = mutableMapOf<String, SubtaskTrees.Staging>()
    private val deletions = mutableMapOf<String, Boolean>()
    private val arrangement = mutableMapOf<String, SubtaskTrees.Arrangement>()

    fun added(key: String) {
        added.add(key)
    }

    fun dropped(key: String) {
        added.remove(key)
    }

    fun edited(displaced: Map<String, SubtaskTrees.Staging>) {
        displaced.forEach { (key, was) -> edited.getOrPut(key) { was } }
    }

    fun deletionStaged(key: String) {
        deletions.getOrPut(key) { trees.get(key)?.deleted ?: false }
    }

    fun rememberArrangement() {
        rememberArrangement(trees.arrangementUnder(rootKey()))
    }

    fun rememberArrangement(arrangements: Map<String, SubtaskTrees.Arrangement>) {
        arrangements.forEach { (key, at) -> arrangement.getOrPut(key) { at } }
    }

    fun discard() {
        added.forEach { trees.drop(it) }
        trees.revert(edited.toMap())
        trees.restoreDeletions(deletions.toMap())
        trees.restoreArrangement(rootKey(), arrangement.toMap())
        clear()
    }

    fun clear() {
        added.clear()
        edited.clear()
        deletions.clear()
        arrangement.clear()
    }

    fun clearWritten(written: Collection<String>) {
        val keys = written.toHashSet()
        added.removeAll(keys)
        edited.keys.removeAll(keys)
        deletions.keys.removeAll(keys)
        arrangement.keys.removeAll(keys)
    }
}
