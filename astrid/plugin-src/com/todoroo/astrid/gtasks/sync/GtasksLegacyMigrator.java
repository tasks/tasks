package com.todoroo.astrid.gtasks.sync;

import java.io.IOException;
import java.util.HashMap;

import com.google.api.services.tasks.v1.model.TaskLists;
import com.google.api.services.tasks.v1.model.Tasks;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.api.GtasksService;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;

/**
 * Class to handle migration of legacy metadata (old remote ids) to new
 * metadata based on the official remote ids returned by the api.
 * @author Sam Bosley
 *
 */
public class GtasksLegacyMigrator {

    @Autowired GtasksMetadataService gtasksMetadataService;
    @Autowired TaskService taskService;
    @Autowired MetadataService metadataService;
    @Autowired GtasksListService gtasksListService;
    @Autowired GtasksPreferenceService gtasksPreferenceService;

    private final GtasksService gtasksService;
    private final GtasksListService listService;
    private final TaskLists allLists;

    static {
        AstridDependencyInjector.initialize();
    }

    public GtasksLegacyMigrator(GtasksService service,GtasksListService listService, TaskLists allLists) {
        DependencyInjectionService.getInstance().inject(this);
        this.gtasksService = service;
        this.listService = listService;
        this.allLists = allLists;
    }

    public void checkAndMigrateLegacy() throws IOException {
        if (!gtasksPreferenceService.migrationHasOccurred()) {

            listService.migrateListIds(allLists);

            //Fetch all tasks that have associated gtask metadata
            String defaultListTitle = gtasksListService.getListName(Preferences.getStringValue(GtasksPreferenceService.PREF_DEFAULT_LIST));
            String defaultListId = null;

            TodorooCursor<Task> allTasksWithGtaskData = taskService.query(Query.select(Task.PROPERTIES).
                    where(Task.ID.in(
                            Query.select(Metadata.TASK).from(Metadata.TABLE).
                            where(Metadata.KEY.eq(GtasksMetadata.METADATA_KEY)))));

            try {
                if (allTasksWithGtaskData.getCount() > 0) {
                    //Fetch all remote tasks from all remote lists (this may be an expensive operation)
                    //and map their titles to their real remote ids
                    HashMap<String, String> taskAndListTitlesToRemoteTaskIds = new HashMap<String, String>();

                    for (com.google.api.services.tasks.v1.model.TaskList list : allLists.items) {
                        if (list.title.equals(defaultListTitle)) {
                            defaultListId = list.id;
                        }

                        Tasks allTasks = gtasksService.getAllGtasksFromListId(list.id, true);

                        if (allTasks.items != null) {
                            for (com.google.api.services.tasks.v1.model.Task t : allTasks.items) {
                                String key = constructKeyFromTitles(t.title, list.title);
                                taskAndListTitlesToRemoteTaskIds.put(key, t.id);
                            }
                        }
                    }

                    //For each local task, check to see if its title paired with any list title has a match in the map
                    while (!allTasksWithGtaskData.isLast()) {
                        allTasksWithGtaskData.moveToNext();
                        GtasksTaskContainer container = gtasksMetadataService.readTaskAndMetadata(allTasksWithGtaskData);

                        //Search through lists to see if one of them has match
                        String taskTitle = container.task.getValue(Task.TITLE);
                        for (com.google.api.services.tasks.v1.model.TaskList list : allLists.items) {
                            String expectedKey = constructKeyFromTitles(taskTitle, list.title);

                            if (taskAndListTitlesToRemoteTaskIds.containsKey(expectedKey)) {
                                String newRemoteTaskId = taskAndListTitlesToRemoteTaskIds.get(expectedKey);
                                String newRemoteListId = list.id;

                                container.gtaskMetadata.setValue(GtasksMetadata.ID, newRemoteTaskId);
                                container.gtaskMetadata.setValue(GtasksMetadata.LIST_ID, newRemoteListId);
                                gtasksMetadataService.saveTaskAndMetadata(container);
                                break;
                            }
                        }
                    }
                    if (defaultListId == null) {
                        com.google.api.services.tasks.v1.model.TaskList defaultList = gtasksService.getGtaskList("@default"); //$NON-NLS-1$
                        defaultListId = defaultList.id;
                    }
                    Preferences.setString(GtasksPreferenceService.PREF_DEFAULT_LIST, defaultListId);
                }
            } finally {
                allTasksWithGtaskData.close();
            }
            Preferences.setBoolean(GtasksPreferenceService.PREF_MIGRATION_HAS_OCCURRED, true); //Record successful migration
        }
    }

    private String constructKeyFromTitles(String taskTitle, String listTitle) {
        return taskTitle + "//" + listTitle; //$NON-NLS-1$
    }

}
