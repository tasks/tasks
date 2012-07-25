/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.api.services.tasks.model.Tasks;
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
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
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

    private final GtasksInvoker gtasksService;
    private final GtasksListService listService;
    private final TaskLists allLists;

    static {
        AstridDependencyInjector.initialize();
    }

    public GtasksLegacyMigrator(GtasksInvoker service,GtasksListService listService, TaskLists allLists) {
        DependencyInjectionService.getInstance().inject(this);
        this.gtasksService = service;
        this.listService = listService;
        this.allLists = allLists;
    }

    public void checkAndMigrateLegacy() throws IOException {
        if (!gtasksPreferenceService.migrationHasOccurred()) {

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

                    List<TaskList> items = allLists.getItems();
                    for (TaskList list : items) {
                        if (list.getTitle().equals(defaultListTitle)) {
                            defaultListId = list.getId();
                        }

                        Tasks allTasks = gtasksService.getAllGtasksFromListId(list.getId(), false, false, 0);

                        List<com.google.api.services.tasks.model.Task> tasksItems = allTasks.getItems();
                        if (tasksItems != null) {
                            for (com.google.api.services.tasks.model.Task t : tasksItems) {
                                String key = constructKeyFromTitles(t.getTitle(), list.getTitle());
                                taskAndListTitlesToRemoteTaskIds.put(key, t.getId());
                            }
                        }
                    }

                    if (defaultListId == null) {
                        com.google.api.services.tasks.model.TaskList defaultList = gtasksService.getGtaskList("@default"); //$NON-NLS-1$
                        defaultListId = defaultList.getId();
                    }
                    Preferences.setString(GtasksPreferenceService.PREF_DEFAULT_LIST, defaultListId);

                    //For each local task, check to see if its title paired with any list title has a match in the map
                    for (allTasksWithGtaskData.moveToFirst(); !allTasksWithGtaskData.isAfterLast(); allTasksWithGtaskData.moveToNext()) {
                        GtasksTaskContainer container = gtasksMetadataService.readTaskAndMetadata(allTasksWithGtaskData);
                        // memorize the original listname for the case that the task is not matched,
                        // then it should at least be recreated in the correct list
                        String originalListName = gtasksListService.getListName(
                                container.gtaskMetadata.getValue(GtasksMetadata.LIST_ID));
                        String originalListId = null;

                        //Search through lists to see if one of them has match
                        String taskTitle = container.task.getValue(Task.TITLE);
                        boolean foundMatch = false;
                        items = allLists.getItems();
                        for (TaskList list : items) {
                            String expectedKey = constructKeyFromTitles(taskTitle, list.getTitle());

                            // save the new id of the current list
                            // if it matches the listname of the current task
                            if (list.getTitle() != null && list.getTitle().equals(originalListName))
                                originalListId = list.getId();

                            if (taskAndListTitlesToRemoteTaskIds.containsKey(expectedKey)) {
                                foundMatch = true;
                                String newRemoteTaskId = taskAndListTitlesToRemoteTaskIds.get(expectedKey);
                                String newRemoteListId = list.getId();

                                container.gtaskMetadata.setValue(GtasksMetadata.ID, newRemoteTaskId);
                                container.gtaskMetadata.setValue(GtasksMetadata.LIST_ID, newRemoteListId);
                                gtasksMetadataService.saveTaskAndMetadata(container);
                                break;
                            }
                        }

                        if (!foundMatch) {
                            //For non-matches, make the task look newly created
                            container.gtaskMetadata = GtasksMetadata.createEmptyMetadata(container.task.getId());
                            container.gtaskMetadata.setValue(GtasksMetadata.ID, ""); //$NON-NLS-1$
                            if (originalListId != null) {
                                // set the list-id based on the original listname, saved above during for-loop
                                container.gtaskMetadata.setValue(GtasksMetadata.LIST_ID, originalListId);
                            } else {
                                // remote list or local list was renamed, so put this unmatched task in the default list
                                container.gtaskMetadata.setValue(GtasksMetadata.LIST_ID, defaultListId);
                            }
                            gtasksMetadataService.saveTaskAndMetadata(container);
                            break;
                        }
                    }
                }

                // migrate the list-id's afterwards, so that we can put the non-matched tasks in their original lists
                // if the listnames didnt change before migration (defaultlist otherwise)
                listService.migrateListIds(allLists);

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
