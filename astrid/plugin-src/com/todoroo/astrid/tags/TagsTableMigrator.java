package com.todoroo.astrid.tags;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskToTagDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TaskToTag;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;

public class TagsTableMigrator {

    @Autowired private MetadataService metadataService;
    @Autowired private TagDataService tagDataService;
    @Autowired private TaskToTagDao taskToTagDao;

    private static final String PREF_MIGRATED_TASKS_TO_TAGS = "tasks_to_tags_migration"; //$NON-NLS-1$

    public TagsTableMigrator() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public void migrateTagMetadataToTable() {
        if (Preferences.getBoolean(PREF_MIGRATED_TASKS_TO_TAGS, false))
            return;

        // First assert that a TagData object exists for each tag metadata
        Query noTagDataQuery = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                MetadataCriteria.withKey(TagService.KEY),
                Criterion.not(TagService.TAG.in(Query.select(TagData.NAME).from(TagData.TABLE))))).groupBy(TagService.TAG);

        TodorooCursor<Metadata> noTagData = metadataService.query(noTagDataQuery);
        try {
            Metadata tag = new Metadata();
            for (noTagData.moveToFirst(); !noTagData.isAfterLast(); noTagData.moveToNext()) {
                tag.readFromCursor(noTagData);

                System.err.println("CREATING TAG DATA " + tag.getValue(TagService.TAG));

                TagData newTagData = new TagData();
                newTagData.setValue(TagData.NAME, tag.getValue(TagService.TAG));
                tagDataService.save(newTagData);
            }
        } finally {
            noTagData.close();
        }

        // Then move all tag metadata to the new table
        Query joinedTagData = Query.select(Metadata.TASK, TagService.TAG, TagData.ID)
                .join(Join.left(TagData.TABLE,
                        Criterion.and(MetadataCriteria.withKey(TagService.KEY), TagService.TAG.eq(TagData.NAME))));

        TodorooCursor<Metadata> allTagLinks = metadataService.query(joinedTagData);
        try {
            Metadata tag = new Metadata();
            for (allTagLinks.moveToFirst(); !allTagLinks.isAfterLast(); allTagLinks.moveToNext()) {
                tag.readFromCursor(allTagLinks);

                // Create new tag links
                TaskToTag link = new TaskToTag();
                System.err.println("LINK from task " + tag.getValue(Metadata.TASK) + " to " + tag.getValue(TagService.TAG));
                link.setValue(TaskToTag.TASK_ID, tag.getValue(Metadata.TASK));
                link.setValue(TaskToTag.TAG_ID, tag.getValue(TagData.ID));

                taskToTagDao.createNew(link);
            }
        } finally {
            allTagLinks.close();
        }

        Preferences.setBoolean(PREF_MIGRATED_TASKS_TO_TAGS, true);
    }

}
