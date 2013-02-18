package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;

@SuppressWarnings("deprecation")
public class SubtasksMigrationTest extends SubtasksTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Preferences.clear(SubtasksUpdater.ACTIVE_TASKS_ORDER);
    }

    /* Starting basic state (see SubtasksTestCase):
    *
    * A
    *  B
    *  C
    *   D
    * E
    * F
    */
    private void createBasicMetadata(TagData tagData) {
        createSubtasksMetadata(tagData, 1, 1, 0);
        createSubtasksMetadata(tagData, 2, 2, 1);
        createSubtasksMetadata(tagData, 3, 3, 1);
        createSubtasksMetadata(tagData, 4, 4, 2);
        createSubtasksMetadata(tagData, 5, 5, 0);
        createSubtasksMetadata(tagData, 6, 6, 0);
    }

    private void createSubtasksMetadata(TagData tagData, long taskId, long order, int indent) {
        Metadata m = new Metadata();
        m.setValue(Metadata.KEY, SubtasksMetadata.METADATA_KEY);
        m.setValue(Metadata.TASK, taskId);
        String tagString = (tagData == null ? SubtasksMetadata.LIST_ACTIVE_TASKS : "td:"+tagData.getId());
        m.setValue(SubtasksMetadata.TAG, tagString);
        m.setValue(SubtasksMetadata.ORDER, order);
        m.setValue(SubtasksMetadata.INDENT, indent);
        PluginServices.getMetadataService().save(m);
    }

    private void basicTest(TagData tagData) {
        createBasicMetadata(tagData);
        SubtasksMetadataMigration migrator = new SubtasksMetadataMigration();
        migrator.performMigration();

        String newSerializedTree = getSerializedTree(tagData);
        String expectedSerializedTree = DEFAULT_SERIALIZED_TREE_STRING;

        assertEquals(expectedSerializedTree, newSerializedTree);
    }

    private String getSerializedTree(TagData tagData) {
        if (tagData == null)
            return Preferences.getStringValue(SubtasksUpdater.ACTIVE_TASKS_ORDER).replaceAll("\\s", "");
        tagData = PluginServices.getTagDataService().fetchById(tagData.getId(), TagData.ID, TagData.TAG_ORDERING);
        return tagData.getValue(TagData.TAG_ORDERING).replaceAll("\\s", "");
    }

    public void testMigrationForActiveTasks() {
        basicTest(null);
    }

    public void testMigrationForTagData() {
        TagData td = new TagData();
        td.setValue(TagData.NAME, "tag");
        PluginServices.getTagDataService().save(td);

        basicTest(td);
    }

    /* Starting advanced state
     *
     * For active tasks
     *
     * A
     *  B
     * C
     *  D
     *   E
     *  F
     *
     *  For tag data
     *
     *  F
     *  E
     *   B
     *   D
     *    C
     *  A
     */

    private static final String ACTIVE_TASKS_TREE = "[\"-1\", [\"1\", \"2\"], [\"3\", [\"4\", \"5\"], \"6\"]]".replaceAll("\\s", "");
    private static final String TAG_DATA_TREE = "[\"-1\", \"6\", [\"5\", \"2\", [\"4\",\"3\"]], \"1\"]".replaceAll("\\s", "");

    private void createAdvancedMetadata(TagData tagData) {
        createSubtasksMetadata(tagData, 6, 1, 0);
        createSubtasksMetadata(tagData, 5, 2, 0);
        createSubtasksMetadata(tagData, 2, 3, 1);
        createSubtasksMetadata(tagData, 4, 4, 1);
        createSubtasksMetadata(tagData, 3, 5, 2);
        createSubtasksMetadata(tagData, 1, 6, 0);

        createSubtasksMetadata(null, 1, 1, 0);
        createSubtasksMetadata(null, 2, 2, 1);
        createSubtasksMetadata(null, 3, 3, 0);
        createSubtasksMetadata(null, 4, 4, 1);
        createSubtasksMetadata(null, 5, 5, 2);
        createSubtasksMetadata(null, 6, 6, 1);
    }

    public void testMigrationWithBothPresent() {
        TagData td = new TagData();
        td.setValue(TagData.NAME, "tag");
        PluginServices.getTagDataService().save(td);

        createAdvancedMetadata(td);

        SubtasksMetadataMigration migrator = new SubtasksMetadataMigration();
        migrator.performMigration();

        assertEquals(TAG_DATA_TREE, getSerializedTree(td));
        assertEquals(ACTIVE_TASKS_TREE, getSerializedTree(null));
    }
}
