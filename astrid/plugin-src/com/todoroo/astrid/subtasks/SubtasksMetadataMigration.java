package com.todoroo.astrid.subtasks;

import java.util.ArrayList;

import android.util.Log;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.subtasks.AstridOrderedListUpdater.Node;

@SuppressWarnings("deprecation") // Subtasks metadata is deprecated
public class SubtasksMetadataMigration {

    @Autowired
    private TagDataService tagDataService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    @Autowired
    private ActFmSyncService actFmSyncService;

    public SubtasksMetadataMigration() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public void performMigration() {
        TodorooCursor<Metadata> subtasksMetadata = metadataService.query(Query.select(Metadata.PROPERTIES)
                .where(MetadataCriteria.withKey(SubtasksMetadata.METADATA_KEY))
                .orderBy(Order.asc(SubtasksMetadata.TAG), Order.asc(Functions.cast(SubtasksMetadata.ORDER, "LONG")))); //$NON-NLS-1$
        try {
            Metadata m = new Metadata();
            for (subtasksMetadata.moveToFirst(); !subtasksMetadata.isAfterLast(); subtasksMetadata.moveToNext()) {
                m.readFromCursor(subtasksMetadata);
                String tag = m.getValue(SubtasksMetadata.TAG);
                processTag(tag, subtasksMetadata);
            }
        } finally {
            subtasksMetadata.close();
        }
    }

    @SuppressWarnings("nls")
    private void processTag(String tag, TodorooCursor<Metadata> subtasksMetadata) {
        Metadata item = new Metadata();
        TagData td = null;
        try {
            if (!SubtasksMetadata.LIST_ACTIVE_TASKS.equals(tag)) {
                String idString = tag.replace("td:", "");
                long id = Long.parseLong(idString);
                td = tagDataService.fetchById(id, TagData.ID);
            }
        } catch (NumberFormatException e) {
            Log.e("subtasks-migration", "Could not parse tag id from " + tag, e);
        }

        if (td == null && !SubtasksMetadata.LIST_ACTIVE_TASKS.equals(tag)) {
            for (; !subtasksMetadata.isAfterLast(); subtasksMetadata.moveToNext()) {
                item.readFromCursor(subtasksMetadata);
                if (!item.getValue(SubtasksMetadata.TAG).equals(tag))
                    break;
            }
        } else {
            String newTree = buildTreeModelFromMetadata(tag, subtasksMetadata);
            if (td != null) {
                td.setValue(TagData.TAG_ORDERING, newTree);
                tagDataService.save(td);
            } else {
                Preferences.setString(SubtasksUpdater.ACTIVE_TASKS_ORDER, newTree);
            }
        }

        subtasksMetadata.moveToPrevious(); // Move back one to undo the last iteration of the for loop
    }

    private String buildTreeModelFromMetadata(String tag, TodorooCursor<Metadata> cursor) {
        Metadata item = new Metadata();
        Node root = new Node("-1", null, -1); //$NON-NLS-1$
        for (; !cursor.isAfterLast(); cursor.moveToNext()) {
            item.clear();
            item.readFromCursor(cursor);
            if (!item.getValue(SubtasksMetadata.TAG).equals(tag))
                break;

            int indent = 0;
            if (item.containsNonNullValue(SubtasksMetadata.INDENT)) {
                Integer i = item.getValue(SubtasksMetadata.INDENT);
                if (i != null)
                    indent = i.intValue();
            }
            Node parent = findNextParentForIndent(root, indent);
            Node newNode = new Node(item.getValue(Metadata.TASK).toString(), parent, parent.indent + 1);
            parent.children.add(newNode);
        }
        return AstridOrderedListUpdater.serializeTree(root);
    }

    private Node findNextParentForIndent(Node root, int indent) {
        if (indent <= 0)
            return root;

        ArrayList<Node> children = root.children;
        if (children.size() == 0)
            return root;

        return findNextParentForIndent(children.get(children.size() - 1), indent - 1);
    }
}
