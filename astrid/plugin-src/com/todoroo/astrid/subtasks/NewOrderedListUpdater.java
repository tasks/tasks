package com.todoroo.astrid.subtasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.Filter;

public abstract class NewOrderedListUpdater<LIST> {

    public NewOrderedListUpdater() {
        DependencyInjectionService.getInstance().inject(this);
        idToNode = new HashMap<Long, Node>();
    }

    public interface OrderedListIterator {
        public void processTask(Node node);
    }

    protected static class Node {
        public final long taskId;
        public Node parent;
        public int indent;
        public final ArrayList<Node> children = new ArrayList<Node>();

        public Node(long taskId, Node parent, int indent) {
            this.taskId = taskId;
            this.parent = parent;
            this.indent = indent;
        }
    }

    private Node treeRoot;

    private final HashMap<Long, Node> idToNode;

    protected abstract String getSerializedTree();

    public int getIndentForTask(long targetTaskId) {
        Node n = idToNode.get(targetTaskId);
        if (n == null)
            return 0;
        return n.indent;
    }

    protected void initialize(LIST list, Filter filter) {
        treeRoot = buildTreeModel(getSerializedTree());
    }

    public Long[] getOrderedIds() {
        ArrayList<Long> ids = new ArrayList<Long>();
        orderedIdHelper(treeRoot, ids);
        return ids.toArray(new Long[ids.size()]);
    }

    private void orderedIdHelper(Node node, List<Long> ids) {
        if (node != treeRoot)
            ids.add(node.taskId);

        for (Node child : node.children) {
            orderedIdHelper(child, ids);
        }
    }

    public void indent(long targetTaskId, int delta) {
        Node node = idToNode.get(targetTaskId);
        indentHelper(node, delta);
    }

    private void indentHelper(Node node, int delta) {
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
            node.indent = newParent.indent + 1;
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
            node.indent = newParent.indent + 1;
            newSiblings.add(insertAfter + 1, node);
        }
    }

    public void moveTo(long targetTaskId, long beforeTaskId) {
        Node target = idToNode.get(targetTaskId);
        if (target == null)
            return;

        if (beforeTaskId == -1) {
            moveToEndOfList(target);
        }

        Node before = idToNode.get(beforeTaskId);

        if (before == null)
            return;

        moveHelper(target, before);
    }

    private void moveHelper(Node moveThis, Node beforeThis) {
        Node parent = beforeThis.parent;
        ArrayList<Node> siblings = parent.children;

        int index = siblings.indexOf(beforeThis);
        if (index < 0)
            return;

        moveThis.parent = parent;
        siblings.add(index, moveThis);
    }

    private void moveToEndOfList(Node moveThis) {
        Node parent = moveThis.parent;
        parent.children.remove(moveThis);
        treeRoot.children.add(moveThis);
        moveThis.parent = treeRoot;
    }

    private Node buildTreeModel(String serializedTree) {
        Node root = new Node(-1, null, -1);
        try {
            JSONArray tree = new JSONArray(serializedTree);
            recursivelyBuildChildren(root, tree);
        } catch (JSONException e) {
            Log.e("OrderedListUpdater", "Error building tree model", e);  //$NON-NLS-1$//$NON-NLS-2$
        }
        return root;
    }

    private void recursivelyBuildChildren(Node node, JSONArray children) throws JSONException {
        for (int i = 0; i < children.length(); i++) {
            JSONObject childObj = children.getJSONObject(i);
            JSONArray keys = childObj.names();
            if (keys == null)
                continue;

            Long id = keys.getLong(0);
            if (id <= 0)
                continue;

            JSONArray childsChildren = childObj.getJSONArray(Long.toString(id));
            Node child = new Node(id, node, node.indent + 1);
            recursivelyBuildChildren(child, childsChildren);
            node.children.add(child);
            idToNode.put(id, child);
        }
    }

    protected String serializeTree() {
        JSONArray tree = new JSONArray();
        if (treeRoot == null) {
            return tree.toString();
        }

        try {
            recursivelySerializeChildren(treeRoot, tree);
        } catch (JSONException e) {
            Log.e("OrderedListUpdater", "Error serializing tree model", e);  //$NON-NLS-1$//$NON-NLS-2$
        }
        return tree.toString();
    }

    private void recursivelySerializeChildren(Node node, JSONArray serializeTo) throws JSONException {
        ArrayList<Node> children = node.children;
        for (Node child : children) {
            JSONObject childObj = new JSONObject();
            JSONArray childsChildren = new JSONArray();
            recursivelySerializeChildren(child, childsChildren);
            childObj.put(Long.toString(child.taskId), childsChildren);
            serializeTo.put(childObj);
        }
    }
}
