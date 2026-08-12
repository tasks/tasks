package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Task
import kotlin.random.Random

class SubtaskTreeModelTest {
    private val root = "root"
    private val rootId = 42L
    private lateinit var trees: SubtaskTrees
    private lateinit var db: FakeRows

    private class FakeRows(private val rootId: Long) {
        private val children = linkedMapOf<Long, MutableList<Long>>()
        private val titles = mutableMapOf<Long, String>()
        private val completed = mutableSetOf<Long>()
        private var nextId = 1L

        fun ids(): List<Long> = children.values.flatten()

        fun add(parent: Long = rootId, title: String): Long {
            val id = ++nextId
            children.getOrPut(parent) { mutableListOf() }.add(id)
            titles[id] = title
            return id
        }

        fun remove(id: Long) {
            val parent = children.entries.firstOrNull { id in it.value }?.key ?: return
            val at = children.getValue(parent).indexOf(id)
            children.getValue(parent).remove(id)
            children.remove(id)?.let { orphans ->
                children.getValue(parent).addAll(at, orphans)
            }
            titles.remove(id)
            completed.remove(id)
        }

        fun reparent(id: Long, parent: Long) {
            if (id == parent || descends(parent, id)) {
                return
            }
            children.entries.firstOrNull { id in it.value }?.value?.remove(id)
            children.getOrPut(parent) { mutableListOf() }.add(id)
        }

        fun reorder(parent: Long, random: Random) {
            children[parent]?.shuffle(random)
        }

        fun setCompleted(id: Long, value: Boolean) {
            if (value) completed.add(id) else completed.remove(id)
        }

        fun rename(id: Long, title: String) {
            titles[id] = title
        }

        private fun descends(id: Long, ancestor: Long): Boolean {
            var current = id
            val seen = mutableSetOf(current)
            while (true) {
                val parent = children.entries.firstOrNull { current in it.value }?.key ?: return false
                if (parent == ancestor) return true
                if (!seen.add(parent)) return false
                current = parent
            }
        }

        fun rows(): List<TaskContainer> {
            val out = mutableListOf<TaskContainer>()
            fun walk(parent: Long, indent: Int) {
                children[parent].orEmpty().forEach { id ->
                    out.add(
                        TaskContainer(
                            task = Task(
                                id = id,
                                title = titles[id],
                                parent = parent,
                                remoteId = "uuid-$id",
                                completionDate = if (id in completed) 1_000L else 0L,
                            ),
                            indent = indent,
                        )
                    )
                    walk(id, indent + 1)
                }
            }
            walk(rootId, 0)
            return out
        }
    }

    private fun nodes() = trees.nodes.value

    private fun ancestorsOf(key: String): List<String> {
        val out = mutableListOf<String>()
        val seen = mutableSetOf(key)
        var current = nodes()[key]?.parentKey
        while (current != null && seen.add(current)) {
            out.add(current)
            current = nodes()[current]?.parentKey
        }
        return out
    }

    private fun isDeleted(key: String) = nodes()[key]?.deleted == true

    private fun isCollapsed(key: String) = nodes()[key]?.task?.isCollapsed == true

    private fun carried(key: String): Boolean? =
        ancestorsOf(key).firstNotNullOfOrNull { nodes()[it]?.stagedCompleted }

    private fun checkInvariants(after: String) {
        val nodes = nodes()
        val rows = trees.rowsOf(root)
        val keys = rows.map { it.key }

        nodes.values.forEach { node ->
            assertTrue(
                "$after: ${node.key} hangs off ${node.parentKey}, which is not in the tree",
                node.parentKey == root || nodes.containsKey(node.parentKey),
            )
        }

        assertEquals("$after: rows are not distinct", keys.size, keys.toSet().size)
        assertEquals("$after: rows do not account for every node", nodes.keys, keys.toSet())

        rows.forEach { row ->
            assertEquals(
                "$after: ${row.key} is drawn at ${row.indent}",
                ancestorsOf(row.key).size - 1,
                row.indent,
            )
        }

        nodes.values.groupBy { it.parentKey }.forEach { (parent, run) ->
            val sequences = run.map { it.sequence }
            assertEquals(
                "$after: $parent's run has repeated positions $sequences",
                sequences.size,
                sequences.toSet().size,
            )
            val drawn = keys.filter { nodes.getValue(it).parentKey == parent }
            assertEquals(
                "$after: $parent's run is drawn out of order",
                run.sortedBy { it.sequence }.map { it.key },
                drawn,
            )
        }

        val expectedDoomed = keys.filter { key ->
            isDeleted(key) || ancestorsOf(key).any { isDeleted(it) }
        }.toSet()
        assertEquals("$after: doomed rows", expectedDoomed, rows.doomed())

        val expectedVisible = keys.filter { key ->
            ancestorsOf(key).none { isCollapsed(it) || isDeleted(it) }
        }
        assertEquals("$after: visible rows", expectedVisible, rows.visible().map { it.key })

        rows.forEach { row ->
            val expected = keys.count { key ->
                val above = ancestorsOf(key)
                row.key in above && above.takeWhile { it != row.key }.none { isDeleted(it) }
            }
            assertEquals("$after: ${row.key} carries", expected, row.children)
        }

        rows.forEach { row ->
            val own = nodes.getValue(row.key).stagedCompleted
                ?: carried(row.key)
                ?: nodes.getValue(row.key).task.isCompleted
            val nested = keys.filter { row.key in ancestorsOf(it) }
            val expected = own && nested.all { key ->
                val theirs = nodes.getValue(key).stagedCompleted
                    ?: carried(key)
                    ?: nodes.getValue(key).task.isCompleted
                theirs
            }
            assertEquals("$after: ${row.key} completion", expected, row.completed)
        }
    }

    private fun newTask(seq: Int) = Task(id = 0, title = "new-$seq", remoteId = "uuid-new-$seq")

    private fun run(seed: Int) {
        val random = Random(seed)
        trees = SubtaskTrees()
        db = FakeRows(rootId)
        repeat(3) { db.add(title = "seed-$it") }
        trees.merge(root, rootId, db.rows())
        checkInvariants("seed $seed: initial merge")

        var added = 0
        repeat(60) { step ->
            val rows = trees.rowsOf(root)
            val visible = rows.visible()
            val keys = rows.map { it.key }
            val what: String
            when (random.nextInt(12)) {
                0 -> {
                    what = "add"
                    trees.add(root, newTask(added++), null)
                }
                1 -> {
                    what = "addAfter"
                    val sibling = keys.randomOrNull(random)?.let { nodes()[it] }
                    if (sibling == null) {
                        trees.add(root, newTask(added++), null)
                    } else {
                        trees.addAfter(sibling, newTask(added++), null)
                    }
                }
                2 -> {
                    what = "setTitle"
                    keys.randomOrNull(random)?.let { trees.setTitle(it, "typed-$step") }
                }
                3 -> {
                    what = "setCompleted"
                    keys.randomOrNull(random)?.let {
                        trees.setCompleted(it, random.nextBoolean())
                    }
                }
                4 -> {
                    what = "delete/restore"
                    keys.randomOrNull(random)?.let {
                        if (random.nextBoolean()) trees.delete(it) else trees.restore(it)
                    }
                }
                5 -> {
                    what = "drop"
                    keys.filter { nodes().getValue(it).isNew }
                        .randomOrNull(random)
                        ?.let { trees.drop(it) }
                }
                6 -> {
                    what = "move"
                    if (visible.size >= 2) {
                        val from = random.nextInt(visible.size)
                        val to = random.nextInt(visible.size)
                        val indent = random.nextInt(-1, 4).takeIf { it >= 0 }
                        visible.resolveMove(from, to, root, indent)?.let { landing ->
                            trees.move(visible[from].key, landing.parentKey, landing.after)
                        }
                    }
                }
                7 -> {
                    what = "indent/outdent"
                    keys.randomOrNull(random)?.let {
                        if (random.nextBoolean()) trees.indent(it) else trees.outdent(root, it)
                    }
                }
                8 -> {
                    what = "setCollapsed"
                    keys.randomOrNull(random)?.let {
                        trees.setCollapsed(it, random.nextBoolean())
                    }
                }
                9 -> {
                    what = "settle"
                    val created = keys
                        .filter { nodes().getValue(it).isNew }
                        .take(2)
                        .associateWith { key ->
                            val node = nodes().getValue(key)
                            val parentId = nodes()[node.parentKey]?.id ?: rootId
                            val id = db.add(
                                parent = if (parentId > 0) parentId else rootId,
                                title = node.title.orEmpty(),
                            )
                            node.task.copy(id = id, title = node.title, remoteId = "uuid-$id")
                        }
                    if (created.isNotEmpty()) {
                        trees.settle(created, created.keys.associateWith { nodes().getValue(it) })
                    }
                }
                10 -> {
                    what = "sync"
                    val ids = db.ids()
                    if (ids.isNotEmpty()) {
                        when (random.nextInt(5)) {
                            0 -> db.rename(ids.random(random), "synced-$step")
                            1 -> db.setCompleted(ids.random(random), random.nextBoolean())
                            2 -> db.reorder(
                                listOf(rootId).plus(ids).random(random),
                                random,
                            )
                            3 -> db.reparent(
                                ids.random(random),
                                listOf(rootId).plus(ids).random(random),
                            )
                            else -> db.remove(ids.random(random))
                        }
                    }
                    if (random.nextInt(4) == 0) {
                        db.add(title = "arrived-$step")
                    }
                    trees.merge(root, rootId, db.rows())
                }
                else -> {
                    what = "merge"
                    trees.merge(root, rootId, db.rows())
                }
            }
            checkInvariants("seed $seed, step $step ($what)")
        }
    }

    @Test
    fun theTreeHoldsItsShapeUnderAnySequenceOfEdits() {
        repeat(200) { run(seed = it) }
    }

    @Test
    fun theTreeHoldsItsShapeUnderMoreSequences() {
        repeat(200) { run(seed = 10_000 + it) }
    }
}

private fun <T> List<T>.randomOrNull(random: Random): T? =
    if (isEmpty()) null else this[random.nextInt(size)]
