package com.todoroo.astrid.subtasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

public abstract class AstridOrderedListUpdater<LIST> {

    @Autowired
    private TaskService taskService;

    public AstridOrderedListUpdater() {
        DependencyInjectionService.getInstance().inject(this);
        idToNode = new HashMap<String, Node>();
    }

    public interface OrderedListNodeVisitor {
        public void visitNode(Node node);
    }

    public static class Node {
        public String uuid;
        public Node parent;
        public int indent;
        public final ArrayList<Node> children = new ArrayList<Node>();

        public Node(String uuid, Node parent, int indent) {
            this.uuid = uuid;
            this.parent = parent;
            this.indent = indent;
        }
    }

    private Node treeRoot;

    private final HashMap<String, Node> idToNode;

    protected abstract String getSerializedTree(LIST list, Filter filter);
    protected abstract void writeSerialization(LIST list, String serialized, boolean shouldQueueSync);
    protected abstract void applyToFilter(Filter filter);

    public int getIndentForTask(String targetTaskId) {
        Node n = idToNode.get(targetTaskId);
        if (n == null)
            return 0;
        return n.indent;
    }

    public void initialize(LIST list, Filter filter) {
        initializeFromSerializedTree(list, filter, getSerializedTree(list, filter));
    }

    public void initializeFromSerializedTree(LIST list, Filter filter, String serializedTree) {
        idToNode.clear();
        treeRoot = buildTreeModel(serializedTree, new JSONTreeModelBuilder() {
            @Override
            public void afterAddNode(Node node) {
                idToNode.put(node.uuid, node);
            }
        });
        verifyTreeModel(list, filter);
    }

    private void verifyTreeModel(LIST list, Filter filter) {
        boolean changedThings = false;
        Set<String> keySet = idToNode.keySet();
        Set<String> currentIds = new HashSet<String>();
        for (String id : keySet) {
            currentIds.add(id);
        }
        Set<String> idsInQuery = new HashSet<String>();
        String sql = filter.getSqlQuery().replaceAll("ORDER BY .*", "");  //$NON-NLS-1$//$NON-NLS-2$
        sql = sql + String.format(" ORDER BY %s", Task.CREATION_DATE); //$NON-NLS-1$
        TodorooCursor<Task> tasks = taskService.fetchFiltered(sql, null, Task.UUID);
        try {
            for (tasks.moveToFirst(); !tasks.isAfterLast(); tasks.moveToNext()) {
                String id = tasks.getString(0);
                idsInQuery.add(id);
                if (idToNode.containsKey(id))
                    continue;

                changedThings = true;
                Node newNode = new Node(id, treeRoot, 0);
                treeRoot.children.add(0, newNode);
                idToNode.put(id, newNode);
            }

            currentIds.removeAll(idsInQuery);
            if (currentIds.size() > 0) {
                removeNodes(currentIds);
                changedThings = true;
            }
        } finally {
            tasks.close();
        }
        if (changedThings)
            writeSerialization(list, serializeTree(), false);
    }

    private void removeNodes(Set<String> idsToRemove) {
        for (String id : idsToRemove) {
            Node node = idToNode.get(id);
            if (node == null)
                continue;

            // Remove node from tree, put all children under parent
            Node parent = node.parent;
            parent.children.remove(node);
            for (Node child : node.children) {
                child.parent = parent;
                parent.children.add(child);
                setNodeIndent(child, parent.indent + 1);
            }
        }
    }

    public Node findNodeForTask(String taskId) {
        return idToNode.get(taskId);
    }

    private String[] getOrderedIds() {
        ArrayList<String> ids = new ArrayList<String>();
        orderedIdHelper(treeRoot, ids);
        return ids.toArray(new String[ids.size()]);
    }

    public String getOrderString() {
        String[] ids = getOrderedIds();
        return buildOrderString(ids);
    }

    public static String buildOrderString(String[] ids) {
        StringBuilder builder = new StringBuilder();
        if (ids.length == 0)
            return "(1)"; //$NON-NLS-1$
        for (int i = ids.length - 1; i >= 0; i--) {
            builder.append(Task.UUID.eq(ids[i]).toString());
            if (i > 0)
                builder.append(", "); //$NON-NLS-1$
        }
        return builder.toString();
    }

    private void orderedIdHelper(Node node, List<String> ids) {
        if (node != treeRoot)
            ids.add(node.uuid);

        for (Node child : node.children) {
            orderedIdHelper(child, ids);
        }
    }

    public void applyToDescendants(String taskId, OrderedListNodeVisitor visitor) {
        Node n = idToNode.get(taskId);
        if (n == null)
            return;
        applyToDescendantsHelper(n, visitor);
    }

    private void applyToDescendantsHelper(Node n, OrderedListNodeVisitor visitor) {
        ArrayList<Node> children = n.children;
        for (Node child : children) {
            visitor.visitNode(child);
            applyToDescendantsHelper(child, visitor);
        }
    }

    public void iterateOverList(OrderedListNodeVisitor visitor) {
        applyToDescendantsHelper(treeRoot, visitor);
    }

    public void indent(LIST list, Filter filter, String targetTaskId, int delta) {
        Node node = idToNode.get(targetTaskId);
        indentHelper(list, filter, node, delta);
    }

    private void indentHelper(LIST list, Filter filter, Node node, int delta) {
        if (node == null)
            return;
        if (delta == 0)
            return;
        Node parent = node.parent;
        if (parent == null)
            return;

        if (delta > 0) {
            ArrayList<Node> siblings = parent.children;
            int index = siblings.indexOf(node);
            if (index <= 0) // Can't indent first child
                return;
            Node newParent = siblings.get(index - 1);
            siblings.remove(index);
            node.parent = newParent;
            newParent.children.add(node);
            setNodeIndent(node, newParent.indent + 1);
        } else if (delta < 0) {
            if (parent == treeRoot) // Can't deindent a top level item
                return;

            ArrayList<Node> siblings = parent.children;
            int index = siblings.indexOf(node);
            if (index < 0)
                return;

            Node newParent = parent.parent;
            ArrayList<Node> newSiblings = newParent.children;
            int insertAfter = newSiblings.indexOf(parent);
            siblings.remove(index);
            node.parent = newParent;
            setNodeIndent(node, newParent.indent + 1);
            newSiblings.add(insertAfter + 1, node);
        }

        writeSerialization(list, serializeTree(), true);
        applyToFilter(filter);
    }

    private void setNodeIndent(Node node, int indent) {
        node.indent = indent;
        adjustDescendantsIndent(node, indent);
    }

    private void adjustDescendantsIndent(Node node, int baseIndent) {
        for (Node child : node.children) {
            child.indent = baseIndent + 1;
            adjustDescendantsIndent(child, child.indent);
        }
    }

    public void moveTo(LIST list, Filter filter, String targetTaskId, String beforeTaskId) {
        Node target = idToNode.get(targetTaskId);
        if (target == null)
            return;

        if ("-1".equals(beforeTaskId)) { //$NON-NLS-1$
            moveToEndOfList(list, filter, target);
            return;
        }

        Node before = idToNode.get(beforeTaskId);

        if (before == null)
            return;

        if (isDescendantOf(before, target))
            return;

        moveHelper(list, filter, target, before);
    }

    public void moveToParentOf(String moveThis, String toParentOfThis) {
        Node target = idToNode.get(toParentOfThis);
        if (target == null)
            return;

        Node toMove = idToNode.get(moveThis);
        if (toMove == null)
            return;

        Node newParent = target.parent;
        Node oldParent = toMove.parent;

        oldParent.children.remove(toMove);
        toMove.parent = newParent;
        newParent.children.add(toMove);
        setNodeIndent(toMove, toMove.parent.indent + 1);
    }

    private void moveHelper(LIST list, Filter filter, Node moveThis, Node beforeThis) {
        Node oldParent = moveThis.parent;
        ArrayList<Node> oldSiblings = oldParent.children;

        Node newParent = beforeThis.parent;
        ArrayList<Node> newSiblings = newParent.children;

        int beforeIndex = newSiblings.indexOf(beforeThis);
        if (beforeIndex < 0)
            return;

        int nodeIndex = oldSiblings.indexOf(moveThis);
        if (nodeIndex < 0)
            return;

        moveThis.parent = newParent;
        setNodeIndent(moveThis, newParent.indent + 1);
        oldSiblings.remove(moveThis);

        if (newSiblings == oldSiblings && beforeIndex > nodeIndex) {
            beforeIndex--;
        }
        newSiblings.add(beforeIndex, moveThis);
        writeSerialization(list, serializeTree(), true);
        applyToFilter(filter);
    }

    // Returns true if desc is a descendant of parent
    private boolean isDescendantOf(Node desc, Node parent) {
        Node curr = desc;
        while (curr != treeRoot) {
            if (curr == parent)
                return true;
            curr = curr.parent;
        }
        return false;
    }

    private void moveToEndOfList(LIST list, Filter filter, Node moveThis) {
        Node parent = moveThis.parent;
        parent.children.remove(moveThis);
        treeRoot.children.add(moveThis);
        moveThis.parent = treeRoot;
        setNodeIndent(moveThis, 0);
        writeSerialization(list, serializeTree(), true);
        applyToFilter(filter);
    }

    public void onCreateTask(LIST list, Filter filter, String uuid) {
        if (idToNode.containsKey(uuid) || !RemoteModel.isValidUuid(uuid))
            return;

        Node newNode = new Node(uuid, treeRoot, 0);
        treeRoot.children.add(0, newNode);
        idToNode.put(uuid, newNode);
        writeSerialization(list, serializeTree(), true);
        applyToFilter(filter);
    }

    public void onDeleteTask(LIST list, Filter filter, String taskId) {
        Node task = idToNode.get(taskId);
        if (task == null)
            return;

        Node parent = task.parent;
        ArrayList<Node> siblings = parent.children;
        int index = siblings.indexOf(task);

        if (index >= 0)
            siblings.remove(index);
        for (Node child : task.children) {
            child.parent = parent;
            siblings.add(index, child);
            setNodeIndent(child, parent.indent + 1);
            index++;
        }
        idToNode.remove(taskId);

        writeSerialization(list, serializeTree(), true);
        applyToFilter(filter);
    }

    private interface JSONTreeModelBuilder {
        void afterAddNode(Node node);
    }

    public static Node buildTreeModel(String serializedTree, JSONTreeModelBuilder callback) {
        Node root = new Node("-1", null, -1); //$NON-NLS-1$
        try {
            JSONArray tree = new JSONArray(serializedTree);
            recursivelyBuildChildren(root, tree, callback);
        } catch (JSONException e) {
            Log.e("OrderedListUpdater", "Error building tree model", e);  //$NON-NLS-1$//$NON-NLS-2$
        }
        return root;
    }

    private static void recursivelyBuildChildren(Node node, JSONArray children, JSONTreeModelBuilder callback) throws JSONException {
        for (int i = 1; i < children.length(); i++) {
            JSONArray subarray = children.optJSONArray(i);
            String uuid = RemoteModel.NO_UUID;
            if (subarray == null)
                uuid = children.getString(i);
            else
                uuid = subarray.getString(0);

            Node child = new Node(uuid, node, node.indent + 1);
            if (subarray != null)
                recursivelyBuildChildren(child, subarray, callback);
            node.children.add(child);
            if (callback != null)
                callback.afterAddNode(child);
        }
    }

    protected String serializeTree() {
        return serializeTree(treeRoot);
    }

    public static String serializeTree(Node root) {
        JSONArray tree = new JSONArray();
        if (root == null) {
            return tree.toString();
        }

        try {
            recursivelySerialize(root, tree);
        } catch (JSONException e) {
            Log.e("OrderedListUpdater", "Error serializing tree model", e);  //$NON-NLS-1$//$NON-NLS-2$
        }
        return tree.toString();
    }

    private static void recursivelySerialize(Node node, JSONArray serializeTo) throws JSONException {
        ArrayList<Node> children = node.children;
        serializeTo.put(node.uuid);
        for (Node child : children) {
            if (child.children.size() > 0) {
                JSONArray branch = new JSONArray();
                recursivelySerialize(child, branch);
                serializeTo.put(branch);
            } else {
                serializeTo.put(child.uuid);
            }
        }
    }
}
