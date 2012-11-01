package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;

@SuppressWarnings("deprecation")
public class SubtasksMigrationTest extends SubtasksTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Preferences.clear(SubtasksUpdater.ACTIVE_TASKS_ORDER);
        createLegacyMetadata();
    }

    /* Starting State (see SubtasksTestCase):
    *
    * A
    *  B
    *  C
    *   D
    * E
    * F
    */
    private void createLegacyMetadata() {
        createSubtasksMetadata(1, 1, 0);
        createSubtasksMetadata(2, 2, 1);
        createSubtasksMetadata(3, 3, 1);
        createSubtasksMetadata(4, 4, 2);
        createSubtasksMetadata(5, 5, 0);
        createSubtasksMetadata(6, 6, 0);
    }

    private void createSubtasksMetadata(long taskId, long order, int indent) {
        Metadata m = new Metadata();
        m.setValue(Metadata.KEY, SubtasksMetadata.METADATA_KEY);
        m.setValue(Metadata.TASK, taskId);
        m.setValue(SubtasksMetadata.TAG, SubtasksMetadata.LIST_ACTIVE_TASKS);
        m.setValue(SubtasksMetadata.ORDER, order);
        m.setValue(SubtasksMetadata.INDENT, indent);
        PluginServices.getMetadataService().save(m);
    }

    public void testMigration() {
        SubtasksMetadataMigration migrator = new SubtasksMetadataMigration();
        migrator.performMigration();

        String newSerializedTree = Preferences.getStringValue(SubtasksUpdater.ACTIVE_TASKS_ORDER).replaceAll("\\s", "");
        String expectedSerializedTree = DEFAULT_SERIALIZED_TREE.replaceAll("\\s", "");

        assertEquals(expectedSerializedTree, newSerializedTree);
    }
}
