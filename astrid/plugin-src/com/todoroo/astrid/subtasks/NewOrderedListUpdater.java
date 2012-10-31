package com.todoroo.astrid.subtasks;

import java.util.ArrayList;
import java.util.HashMap;

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
        public final ArrayList<Node> children = new ArrayList<Node>();

        public Node(long taskId, Node parent) {
            this.taskId = taskId;
            this.parent = parent;
        }
    }

    private Node treeRoot;

    private final HashMap<Long, Node> idToNode;

    protected abstract String getSerializedTree();

    protected void initialize(LIST list, Filter filter) {
        treeRoot = buildTreeModel(getSerializedTree());
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
            newSiblings.add(insertAfter + 1, node);
        }
    }

    public void moveTo(long targetTaskId, long beforeTaskId) {
        Node target = idToNode.get(targetTaskId);
        Node before = idToNode.get(beforeTaskId);

        if (target == null || before == null)
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

    private Node buildTreeModel(String serializedTree) {
        Node root = new Node(-1, null);
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
            Node child = new Node(id, node);
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
