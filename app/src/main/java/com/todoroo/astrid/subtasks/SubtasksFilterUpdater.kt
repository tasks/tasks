package com.todoroo.astrid.subtasks

import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.Companion.isValidUuid
import org.json.JSONArray
import org.json.JSONException
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.entity.TaskListMetadata
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.db.QueryUtils.showHiddenAndCompleted
import org.tasks.filters.AstridOrderingFilter
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class SubtasksFilterUpdater @Inject constructor(
    private val taskListMetadataDao: TaskListMetadataDao,
    private val taskDao: TaskDao
) {
    private val idToNode = HashMap<String, Node?>()
    private var treeRoot: Node? = null
    private fun getSerializedTree(list: TaskListMetadata?): String? {
        if (list == null) {
            return "[]" // $NON-NLS-1$
        }
        var order = list.taskIds
        if (isNullOrEmpty(order) || "null" == order) // $NON-NLS-1$
        {
            order = "[]" // $NON-NLS-1$
        }
        return order
    }

    suspend fun writeSerialization(list: TaskListMetadata?, serialized: String?) {
        if (list != null) {
            list.taskIds = serialized
            taskListMetadataDao.update(list)
        }
    }

    suspend fun initialize(list: TaskListMetadata?, filter: AstridOrderingFilter) {
        initializeFromSerializedTree(list, filter, getSerializedTree(list))
        applyToFilter(filter)
    }

    private fun applyToFilter(filter: AstridOrderingFilter) {
        var query = filter.getSqlQuery()
        query = query.replace("ORDER BY .*".toRegex(), "")
        query += "ORDER BY $orderString"
        query = showHiddenAndCompleted(query)
        filter.filterOverride = query
    }

    fun getIndentForTask(targetTaskId: String?): Int {
        val n = idToNode[targetTaskId] ?: return 0
        return n.indent
    }

    suspend fun initializeFromSerializedTree(list: TaskListMetadata?, filter: AstridOrderingFilter, serializedTree: String?) {
        idToNode.clear()
        treeRoot = buildTreeModel(serializedTree) { node -> node?.let { idToNode[it.uuid] = it } }
        verifyTreeModel(list, filter)
    }

    private suspend fun verifyTreeModel(list: TaskListMetadata?, filter: AstridOrderingFilter) {
        var changedThings = false
        val keySet: Set<String> = idToNode.keys
        val currentIds: MutableSet<String> = HashSet(keySet)
        val idsInQuery: MutableSet<String> = HashSet()
        var sql = filter.getSqlQuery().replace("ORDER BY .*".toRegex(), "") // $NON-NLS-1$//$NON-NLS-2$
        sql = "$sql ORDER BY created" // $NON-NLS-1$
        sql = showHiddenAndCompleted(sql)
        val tasks = taskDao.fetchFiltered(sql)
        for (task in tasks) {
            val id = task.uuid
            idsInQuery.add(id)
            if (idToNode.containsKey(id)) {
                continue
            }
            changedThings = true
            val newNode = Node(id, treeRoot, 0)
            treeRoot!!.children.add(0, newNode)
            idToNode[id] = newNode
        }
        currentIds.removeAll(idsInQuery)
        if (currentIds.size > 0) {
            removeNodes(currentIds)
            changedThings = true
        }
        if (changedThings) {
            writeSerialization(list, serializeTree())
        }
    }

    private fun removeNodes(idsToRemove: Set<String>) {
        for (id in idsToRemove) {
            val node = idToNode[id] ?: continue

            // Remove node from tree, put all children under parent
            val parent = node.parent
            parent!!.children.remove(node)
            for (child in node.children) {
                child.parent = parent
                parent.children.add(child)
                setNodeIndent(child, parent.indent + 1)
            }
        }
    }

    fun findNodeForTask(taskId: String?): Node? {
        return idToNode[taskId]
    }

    private val orderedIds: List<String>
        get() {
            val ids = ArrayList<String>()
            orderedIdHelper(treeRoot, ids)
            return ids
        }

    private val orderString: String
        get() {
            val ids = orderedIds
            return buildOrderString(ids)
        }

    private fun orderedIdHelper(node: Node?, ids: MutableList<String>) {
        if (node !== treeRoot) {
            ids.add(node!!.uuid)
        }
        for (child in node!!.children) {
            orderedIdHelper(child, ids)
        }
    }

    suspend fun applyToDescendants(taskId: String?, visitor: suspend (Node) -> Unit) {
        val n = idToNode[taskId] ?: return
        applyToDescendantsHelper(n, visitor)
    }

    private suspend fun applyToDescendantsHelper(n: Node, visitor: suspend (Node) -> Unit) {
        val children = n.children
        for (child in children) {
            visitor(child)
            applyToDescendantsHelper(child, visitor)
        }
    }

    suspend fun indent(list: TaskListMetadata, filter: AstridOrderingFilter, targetTaskId: String?, delta: Int) {
        val node = idToNode[targetTaskId]
        indentHelper(list, filter, node, delta)
    }

    private suspend fun indentHelper(list: TaskListMetadata, filter: AstridOrderingFilter, node: Node?, delta: Int) {
        if (node == null) {
            return
        }
        if (delta == 0) {
            return
        }
        val parent = node.parent ?: return
        if (delta > 0) {
            val siblings = parent.children
            val index = siblings.indexOf(node)
            if (index <= 0) // Can't indent first child
            {
                return
            }
            val newParent = siblings[index - 1]
            siblings.removeAt(index)
            node.parent = newParent
            newParent.children.add(node)
            setNodeIndent(node, newParent.indent + 1)
        } else if (delta < 0) {
            if (parent === treeRoot) // Can't deindent a top level item
            {
                return
            }
            val siblings = parent.children
            val index = siblings.indexOf(node)
            if (index < 0) {
                return
            }
            val newParent = parent.parent
            val newSiblings = newParent!!.children
            val insertAfter = newSiblings.indexOf(parent)
            siblings.removeAt(index)
            node.parent = newParent
            setNodeIndent(node, newParent.indent + 1)
            newSiblings.add(insertAfter + 1, node)
        }
        writeSerialization(list, serializeTree())
        applyToFilter(filter)
    }

    private fun setNodeIndent(node: Node, indent: Int) {
        node.indent = indent
        adjustDescendantsIndent(node, indent)
    }

    private fun adjustDescendantsIndent(node: Node, baseIndent: Int) {
        for (child in node.children) {
            child.indent = baseIndent + 1
            adjustDescendantsIndent(child, child.indent)
        }
    }

    suspend fun moveTo(list: TaskListMetadata, filter: AstridOrderingFilter, targetTaskId: String?, beforeTaskId: String) {
        val target = idToNode[targetTaskId] ?: return
        if ("-1" == beforeTaskId) { // $NON-NLS-1$
            moveToEndOfList(list, filter, target)
            return
        }
        val before = idToNode[beforeTaskId] ?: return
        if (isDescendantOf(before, target)) {
            return
        }
        moveHelper(list, filter, target, before)
    }

    fun moveToParentOf(moveThis: String?, toParentOfThis: String?) {
        val target = idToNode[toParentOfThis] ?: return
        val toMove = idToNode[moveThis] ?: return
        val newParent = target.parent
        val oldParent = toMove.parent
        oldParent!!.children.remove(toMove)
        toMove.parent = newParent
        newParent!!.children.add(toMove)
        setNodeIndent(toMove, toMove.parent!!.indent + 1)
    }

    private suspend fun moveHelper(list: TaskListMetadata, filter: AstridOrderingFilter, moveThis: Node, beforeThis: Node) {
        val oldParent = moveThis.parent
        val oldSiblings = oldParent!!.children
        val newParent = beforeThis.parent
        val newSiblings = newParent!!.children
        var beforeIndex = newSiblings.indexOf(beforeThis)
        if (beforeIndex < 0) {
            return
        }
        val nodeIndex = oldSiblings.indexOf(moveThis)
        if (nodeIndex < 0) {
            return
        }
        moveThis.parent = newParent
        setNodeIndent(moveThis, newParent.indent + 1)
        oldSiblings.remove(moveThis)
        if (newSiblings === oldSiblings && beforeIndex > nodeIndex) {
            beforeIndex--
        }
        newSiblings.add(beforeIndex, moveThis)
        writeSerialization(list, serializeTree())
        applyToFilter(filter)
    }

    fun isDescendantOf(desc: String?, parent: String?): Boolean {
        return isDescendantOf(idToNode[desc], idToNode[parent])
    }

    // Returns true if desc is a descendant of parent
    private fun isDescendantOf(desc: Node?, parent: Node?): Boolean {
        var curr = desc
        while (curr !== treeRoot) {
            if (curr === parent) {
                return true
            }
            curr = curr!!.parent
        }
        return false
    }

    private suspend fun moveToEndOfList(list: TaskListMetadata, filter: AstridOrderingFilter, moveThis: Node) {
        val parent = moveThis.parent
        parent!!.children.remove(moveThis)
        treeRoot!!.children.add(moveThis)
        moveThis.parent = treeRoot
        setNodeIndent(moveThis, 0)
        writeSerialization(list, serializeTree())
        applyToFilter(filter)
    }

    suspend fun onCreateTask(list: TaskListMetadata?, filter: AstridOrderingFilter, uuid: String) {
        if (idToNode.containsKey(uuid) || !isValidUuid(uuid)) {
            return
        }
        val newNode = Node(uuid, treeRoot, 0)
        treeRoot!!.children.add(0, newNode)
        idToNode[uuid] = newNode
        writeSerialization(list, serializeTree())
        applyToFilter(filter)
    }

    suspend fun onDeleteTask(list: TaskListMetadata?, filter: AstridOrderingFilter, taskId: String?) {
        val task = idToNode[taskId] ?: return
        val parent = task.parent
        val siblings = parent!!.children
        var index = siblings.indexOf(task)
        if (index >= 0) {
            siblings.removeAt(index)
        }
        for (child in task.children) {
            child.parent = parent
            siblings.add(index, child)
            setNodeIndent(child, parent.indent + 1)
            index++
        }
        idToNode.remove(taskId)
        writeSerialization(list, serializeTree())
        applyToFilter(filter)
    }

    fun serializeTree(): String {
        return serializeTree(treeRoot)
    }

    class Node internal constructor(var uuid: String, var parent: Node?, var indent: Int) {
        val children = ArrayList<Node>()
    }

    companion object {
        const val ACTIVE_TASKS_ORDER = "active_tasks_order" // $NON-NLS-1$
        const val TODAY_TASKS_ORDER = "today_tasks_order" // $NON-NLS-1$
        private const val MAX_ORDERED_TASKS = 900

        fun buildOrderString(ids: List<String>): String {
            val builder = StringBuilder()
            if (ids.isEmpty()) {
                return "(1)" // $NON-NLS-1$
            }
            val indices = ids.indices.reversed().take(MAX_ORDERED_TASKS)
            indices.forEach { i ->
                builder.append(Task.UUID.eq(ids[i]).toString())
                if (i != indices.last()) {
                    builder.append(", ") // $NON-NLS-1$
                }
            }
            return builder.toString()
        }

        fun buildTreeModel(serializedTree: String?, callback: ((Node?) -> Unit)?): Node {
            val root = Node("-1", null, -1) // $NON-NLS-1$
            try {
                val tree = JSONArray(serializedTree)
                recursivelyBuildChildren(root, tree, callback)
            } catch (e: JSONException) {
                Timber.e(e)
            }
            return root
        }

        @Throws(JSONException::class)
        private fun recursivelyBuildChildren(
                node: Node, children: JSONArray, callback: ((Node?) -> Unit)?) {
            for (i in 1 until children.length()) {
                val subarray = children.optJSONArray(i)
                val uuid: String = if (subarray == null) {
                    children.getString(i)
                } else {
                    subarray.getString(0)
                }
                val child = Node(uuid, node, node.indent + 1)
                subarray?.let { recursivelyBuildChildren(child, it, callback) }
                node.children.add(child)
                callback?.invoke(child)
            }
        }

        fun serializeTree(root: Node?): String {
            val tree = JSONArray()
            if (root == null) {
                return tree.toString()
            }
            recursivelySerialize(root, tree)
            return tree.toString()
        }

        private fun recursivelySerialize(node: Node, serializeTo: JSONArray) {
            val children = node.children
            serializeTo.put(node.uuid)
            for (child in children) {
                if (child.children.size > 0) {
                    val branch = JSONArray()
                    recursivelySerialize(child, branch)
                    serializeTo.put(branch)
                } else {
                    serializeTo.put(child.uuid)
                }
            }
        }
    }
}