/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.activities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.timsu.astrid.R;
import com.timsu.astrid.activities.MainActivity.SubActivities;
import com.timsu.astrid.activities.MainActivity.SubActivity;
import com.timsu.astrid.activities.TaskListAdapter.TaskListAdapterHooks;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForList;
import com.timsu.astrid.sync.Synchronizer;
import com.timsu.astrid.sync.Synchronizer.SynchronizerListener;
import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.DialogUtilities;
import com.timsu.astrid.utilities.Preferences;
import com.timsu.astrid.widget.NNumberPickerDialog.OnNNumberPickedListener;


/** 
 * Primary view for the Astrid Application. Lists all of the tasks in the
 * system, and allows users to edit them.
 *
 * @author Tim Su (timsu@stanfordalumni.org)
 *
 */
public class TaskList extends SubActivity {

    // bundle tokens
    public static final String     TAG_TOKEN             = "tag";

    // activities
    private static final int       ACTIVITY_CREATE       = 0;
    private static final int       ACTIVITY_VIEW         = 1;
    private static final int       ACTIVITY_EDIT         = 2;
    private static final int       ACTIVITY_TAGS         = 3;
    private static final int       ACTIVITY_SYNCHRONIZE  = 4;

    // menu codes
    private static final int       INSERT_ID             = Menu.FIRST;
    private static final int       FILTERS_ID            = Menu.FIRST + 1;
    private static final int       TAGS_ID               = Menu.FIRST + 2;
    private static final int       SYNC_ID               = Menu.FIRST + 3;
    private static final int       MORE_ID               = Menu.FIRST + 4;

    private static final int       OPTIONS_SYNC_ID       = Menu.FIRST + 10;
    private static final int       OPTIONS_SETTINGS_ID   = Menu.FIRST + 11;
    private static final int       OPTIONS_HELP_ID       = Menu.FIRST + 12;

    private static final int       CONTEXT_FILTER_HIDDEN = Menu.FIRST + 20;
    private static final int       CONTEXT_FILTER_DONE   = Menu.FIRST + 21;
    private static final int       CONTEXT_FILTER_TAG    = Menu.FIRST + 22;
    private static final int       CONTEXT_SORT_AUTO     = Menu.FIRST + 23;
    private static final int       CONTEXT_SORT_ALPHA    = Menu.FIRST + 24;
    private static final int       CONTEXT_SORT_DUEDATE  = Menu.FIRST + 25;
    private static final int       CONTEXT_SORT_REVERSE  = Menu.FIRST + 26;
    private static final int       CONTEXT_SORT_GROUP    = Menu.FIRST;

    private static final int       SORTFLAG_FILTERDONE   = (1 << 5);
    private static final int       SORTFLAG_FILTERHIDDEN = (1 << 6);

    // UI components
    private ListView listView;
    private Button addButton;
    private View layout;

    // other instance variables
    private Map<TagIdentifier, TagModelForView> tagMap;
    private ArrayList<TaskModelForList> taskArray;
    private HashMap<TaskModelForList, LinkedList<TagModelForView>> taskTags;

    // display filters
    private static boolean filterShowHidden = false;
    private static boolean filterShowDone = false;
    private static TagModelForView filterTag = null;
    private static SortMode sortMode = SortMode.AUTO;
    private static boolean sortReverse = false;

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */
    
    public TaskList(MainActivity parent, SubActivities code, View view) {
    	super(parent, code, view);
    }
    
    @Override
    /** Called when loading up the activity */
    public void onDisplay(Bundle variables) {
    	// load tag map
        tagMap = getTagController().getAllTagsAsMap(getParent());
        
        // process the tag to filter on, if any
        if(variables != null && variables.containsKey(TAG_TOKEN)) {
            TagIdentifier identifier = new TagIdentifier(variables.getLong(TAG_TOKEN));
            filterTag = tagMap.get(identifier);
        }
    	
        setupUIComponents();
        loadTaskListSort();
        fillData();
    }

	public void setupUIComponents() {
        listView = (ListView)findViewById(R.id.tasklist);
        addButton = (Button)findViewById(R.id.addtask);
        addButton.setOnClickListener(new
                View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createTask();
            }
        });

        layout = findViewById(R.id.tasklist_layout);
        layout.setOnCreateContextMenuListener(
                new OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v,
                            ContextMenuInfo menuInfo) {
                        if(menu.hasVisibleItems())
                            return;
                        onCreateMoreOptionsMenu(menu);
                    }
                });
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item;

        item = menu.add(Menu.NONE, INSERT_ID, Menu.NONE,
                R.string.taskList_menu_insert);
        item.setIcon(android.R.drawable.ic_menu_add);
        item.setAlphabeticShortcut('n');

        item = menu.add(Menu.NONE, FILTERS_ID, Menu.NONE,
                R.string.taskList_menu_filters);
        item.setIcon(android.R.drawable.ic_menu_view);
        item.setAlphabeticShortcut('f');

        item = menu.add(Menu.NONE, TAGS_ID, Menu.NONE,
                R.string.taskList_menu_tags);
        item.setIcon(android.R.drawable.ic_menu_myplaces);
        item.setAlphabeticShortcut('t');

        if(Preferences.shouldDisplaySyncButton(getParent())){
            item = menu.add(Menu.NONE, SYNC_ID, Menu.NONE,
                    R.string.taskList_menu_syncshortcut);
            item.setIcon(android.R.drawable.ic_menu_upload);
            item.setAlphabeticShortcut('s');
        }

        item = menu.add(Menu.NONE, MORE_ID, Menu.NONE,
                R.string.taskList_menu_more);
        item.setIcon(android.R.drawable.ic_menu_more);
        item.setAlphabeticShortcut('m');

        return true;
    }

    public boolean onCreateMoreOptionsMenu(Menu menu) {
        MenuItem item;

        item = menu.add(Menu.NONE, OPTIONS_SYNC_ID, Menu.NONE,
                R.string.taskList_menu_sync);
        item.setAlphabeticShortcut('s');

        item = menu.add(Menu.NONE, OPTIONS_SETTINGS_ID, Menu.NONE,
                R.string.taskList_menu_settings);
        item.setAlphabeticShortcut('p');

        item = menu.add(Menu.NONE, OPTIONS_HELP_ID, Menu.NONE,
                R.string.taskList_menu_help);
        item.setAlphabeticShortcut('h');

        return true;
    }

    private enum SortMode {
        ALPHA {
            @Override
            int compareTo(TaskModelForList arg0, TaskModelForList arg1) {
                return arg0.getName().compareTo(arg1.getName());
            }
        },
        DUEDATE {
            long getDueDate(TaskModelForList task) {
                Date definite = task.getDefiniteDueDate();
                Date preferred = task.getPreferredDueDate();
                if(definite != null && preferred != null) {
                    if(preferred.before(new Date()))
                        return definite.getTime();
                    return preferred.getTime();
                } else if(definite != null)
                        return definite.getTime();
                else if(preferred != null)
                    return preferred.getTime();
                else
                    return new Date(2020,1,1).getTime();
            }
            @Override
            int compareTo(TaskModelForList arg0, TaskModelForList arg1) {
                return (int)((getDueDate(arg0) - getDueDate(arg1))/1000);
            }
        },
        AUTO {
            @Override
            int compareTo(TaskModelForList arg0, TaskModelForList arg1) {
                return arg0.getTaskWeight() - arg1.getTaskWeight();
            }
        };

        abstract int compareTo(TaskModelForList arg0, TaskModelForList arg1);
    };

    /* ======================================================================
     * ====================================================== populating list
     * ====================================================================== */

    private boolean isTaskHidden(TaskModelForList task) {
        if(task.isHidden())
            return true;

        if(filterTag == null) {
            for(TagModelForView tags : taskTags.get(task)) {
                if(tags.shouldHideFromMainList())
                    return true;
            }
        }

        return false;
    }

    /** Fill in the Task List with our tasks */
    private void fillData() {
        Resources r = getResources();

        // get a cursor to the task list
        Cursor tasksCursor;
        if(filterTag != null) {
            List<TaskIdentifier> tasks = getTagController().getTaggedTasks(getParent(),
                    filterTag.getTagIdentifier());
            tasksCursor = getTaskController().getTaskListCursorById(tasks);
        } else {
            if(filterShowDone)
                tasksCursor = getTaskController().getAllTaskListCursor();
            else
                tasksCursor = getTaskController().getActiveTaskListCursor();
        }
        startManagingCursor(tasksCursor);
        taskArray = getTaskController().createTaskListFromCursor(tasksCursor);

        // read tags and apply filters
        int hiddenTasks = 0; // # of tasks hidden
        int completedTasks = 0; // # of tasks on list that are done
        taskTags = new HashMap<TaskModelForList, LinkedList<TagModelForView>>();
        for(Iterator<TaskModelForList> i = taskArray.iterator(); i.hasNext();) {
            TaskModelForList task = i.next();

            if(task.isTaskCompleted()) {
                if(!filterShowDone) {
                    i.remove();
                    continue;
                }
            }

            // get list of tags
            LinkedList<TagIdentifier> tagIds = getTagController().getTaskTags(getParent(),
                    task.getTaskIdentifier());
            LinkedList<TagModelForView> tags = new LinkedList<TagModelForView>();
            for(TagIdentifier tagId : tagIds) {
                TagModelForView tag = tagMap.get(tagId);
                tags.add(tag);
            }
            taskTags.put(task, tags);

            // hide hidden
            if(!filterShowHidden) {
                if(isTaskHidden(task)) {
                    hiddenTasks++;
                    i.remove();
                    continue;
                }
            }

            if(task.isTaskCompleted())
                completedTasks++;
        }
        int activeTasks = taskArray.size() - completedTasks;

        // sort task list
        // do sort
        Collections.sort(taskArray, new Comparator<TaskModelForList>() {
            @Override
            public int compare(TaskModelForList arg0, TaskModelForList arg1) {
                return sortMode.compareTo(arg0, arg1);
            }
        });
        if(sortReverse)
            Collections.reverse(taskArray);

        // hide "add" button if we have a few tasks
        if(taskArray.size() > 4)
            addButton.setVisibility(View.GONE);
        else
            addButton.setVisibility(View.VISIBLE);

        // set up the title
        StringBuilder title = new StringBuilder().
            append(r.getString(R.string.taskList_titlePrefix)).append(" ");
        if(filterTag != null) {
            title.append(r.getString(R.string.taskList_titleTagPrefix,
                    filterTag.getName())).append(" ");
        }

        if(completedTasks > 0)
            title.append(r.getQuantityString(R.plurals.NactiveTasks,
                    activeTasks, activeTasks, taskArray.size()));
        else
            title.append(r.getQuantityString(R.plurals.Ntasks,
                    taskArray.size(), taskArray.size()));
        if(hiddenTasks > 0)
            title.append(" (+").append(hiddenTasks).append(" ").
            append(r.getString(R.string.taskList_hiddenSuffix)).append(")");
        setTitle(title);

        setUpListUI();
    }

    private void setUpListUI() {
     // set up our adapter
        TaskListAdapter tasks = new TaskListAdapter(getParent(),
                    R.layout.task_list_row, taskArray, new TaskListAdapterHooks() {
                @Override
                public TagController getTagController() {
                    return getTagController();
                }

                @Override
                public List<TagModelForView> getTagsFor(
                        TaskModelForList task) {
                    return taskTags.get(task);
                }

                @Override
                public List<TaskModelForList> getTaskArray() {
                    return taskArray;
                }

                @Override
                public TaskController getTaskController() {
                    return getTaskController();
                }

                @Override
                public void performItemClick(View v, int position) {
                    listView.performItemClick(v, position, 0);
                }

                public void onCreatedTaskListView(View v, TaskModelForList task) {
                    v.setOnTouchListener(getGestureListener());
                }
        });
        listView.setAdapter(tasks);
        listView.setItemsCanFocus(true);

        // list view listener
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                TaskModelForList task = (TaskModelForList)view.getTag();

                Intent intent = new Intent(getParent(), TaskView.class);
                intent.putExtra(TaskEdit.LOAD_INSTANCE_TOKEN, task.
                        getTaskIdentifier().getId());
                launchActivity(intent, ACTIVITY_VIEW);
            }
        });

        // filters context menu
        listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                if(menu.hasVisibleItems())
                    return;
                Resources r = getResources();
                menu.setHeaderTitle(R.string.taskList_filter_title);

                MenuItem item = menu.add(Menu.NONE, CONTEXT_FILTER_HIDDEN,
                        Menu.NONE, R.string.taskList_filter_hidden);
                item.setCheckable(true);
                item.setChecked(filterShowHidden);

                item = menu.add(Menu.NONE, CONTEXT_FILTER_DONE, Menu.NONE,
                        R.string.taskList_filter_done);
                item.setCheckable(true);
                item.setChecked(filterShowDone);

                if(filterTag != null) {
                    item = menu.add(Menu.NONE, CONTEXT_FILTER_TAG, Menu.NONE,
                            r.getString(R.string.taskList_filter_tagged,
                                    filterTag.getName()));
                    item.setCheckable(true);
                    item.setChecked(true);
                }

                item = menu.add(CONTEXT_SORT_GROUP, CONTEXT_SORT_AUTO, Menu.NONE,
                        R.string.taskList_sort_auto);
                item.setChecked(sortMode == SortMode.AUTO);
                item = menu.add(CONTEXT_SORT_GROUP, CONTEXT_SORT_ALPHA, Menu.NONE,
                        R.string.taskList_sort_alpha);
                item.setChecked(sortMode == SortMode.ALPHA);
                item = menu.add(CONTEXT_SORT_GROUP, CONTEXT_SORT_DUEDATE, Menu.NONE,
                        R.string.taskList_sort_duedate);
                item.setChecked(sortMode == SortMode.DUEDATE);
                menu.setGroupCheckable(CONTEXT_SORT_GROUP, true, true);

                item = menu.add(CONTEXT_SORT_GROUP, CONTEXT_SORT_REVERSE, Menu.NONE,
                        R.string.taskList_sort_reverse);
                item.setCheckable(true);
                item.setChecked(sortReverse);
            }
        });

        listView.setOnTouchListener(getGestureListener());
    }

    /* ======================================================================
     * ======================================================= event handlers
     * ====================================================================== */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Constants.RESULT_SYNCHRONIZE) {
            Synchronizer.synchronize(getParent(), false, new SynchronizerListener() {
                @Override
                public void onSynchronizerFinished(int numServicesSynced) {
                    if(numServicesSynced == 0)
                        DialogUtilities.okDialog(getParent(), getResources().getString(
                                R.string.sync_no_synchronizers), null);
                    fillData();
                }
            });
        } else if(requestCode == ACTIVITY_TAGS)
            switchToActivity(SubActivities.TAG_LIST, null);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // refresh, since stuff might have changed...
        if(hasFocus) {
            fillData();
        }
    }

    private void createTask() {
        Intent intent = new Intent(getParent(), TaskEdit.class);
        if(filterTag != null)
            intent.putExtra(TaskEdit.TAG_NAME_TOKEN, filterTag.getName());
        launchActivity(intent, ACTIVITY_CREATE);
    }

    private void deleteTask(final TaskIdentifier taskId) {
        new AlertDialog.Builder(getParent())
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_this_task_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getTaskController().deleteTask(taskId);
                    fillData();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    public void showTagsView() {
        switchToActivity(SubActivities.TAG_LIST, null);
    }

    /** Save the sorting mode to the preferences */
    private void saveTaskListSort() {
        int sortId = sortMode.ordinal() + 1;

        if(filterShowDone)
            sortId |= SORTFLAG_FILTERDONE;
        if(filterShowHidden)
            sortId |= SORTFLAG_FILTERHIDDEN;

        if(sortReverse)
            sortId *= -1;

        Preferences.setTaskListSort(getParent(), sortId);
    }

    /** Save the sorting mode to the preferences */
    private void loadTaskListSort() {
        int sortId = Preferences.getTaskListSort(getParent());
        if(sortId == 0)
            return;
        sortReverse = sortId < 0;
        sortId = Math.abs(sortId);

        filterShowDone = (sortId & SORTFLAG_FILTERDONE) > 0;
        filterShowHidden = (sortId & SORTFLAG_FILTERHIDDEN) > 0;

        sortId = sortId & ~(SORTFLAG_FILTERDONE | SORTFLAG_FILTERHIDDEN);

        sortMode = SortMode.values()[sortId - 1];
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        final TaskModelForList task;
        Resources r = getResources();

        switch(item.getItemId()) {
        case INSERT_ID:
            createTask();
            return true;
        case FILTERS_ID:
            listView.showContextMenu();
            return true;
        case TAGS_ID:
            showTagsView();
            return true;
        case SYNC_ID:
            onActivityResult(ACTIVITY_SYNCHRONIZE, Constants.RESULT_SYNCHRONIZE, null);
            return true;
        case MORE_ID:
            layout.showContextMenu();
            return true;

        case OPTIONS_SYNC_ID:
            launchActivity(new Intent(getParent(), SyncPreferences.class),
                    ACTIVITY_SYNCHRONIZE);
            return true;
        case OPTIONS_SETTINGS_ID:
        	launchActivity(new Intent(getParent(), EditPreferences.class), 0);
            return true;
        case OPTIONS_HELP_ID:
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(Constants.HELP_URL));
            launchActivity(browserIntent, 0);
            return true;

        case TaskListAdapter.CONTEXT_EDIT_ID:
            task = taskArray.get(item.getGroupId());
            intent = new Intent(getParent(), TaskEdit.class);
            intent.putExtra(TaskEdit.LOAD_INSTANCE_TOKEN, task.getTaskIdentifier().getId());
            launchActivity(intent, ACTIVITY_EDIT);
            return true;
        case TaskListAdapter.CONTEXT_DELETE_ID:
            task = taskArray.get(item.getGroupId());
            deleteTask(task.getTaskIdentifier());
            return true;
        case TaskListAdapter.CONTEXT_TIMER_ID:
            task = taskArray.get(item.getGroupId());
            if(task.getTimerStart() == null)
                task.setTimerStart(new Date());
            else {
                task.stopTimerAndUpdateElapsedTime();
            }
            getTaskController().saveTask(task);
            fillData();
            return true;
        case TaskListAdapter.CONTEXT_POSTPONE_ID:
            task = taskArray.get(item.getGroupId());
            DialogUtilities.dayHourPicker(getParent(),
                r.getString(R.string.taskList_postpone_dialog),
                new OnNNumberPickedListener() {
                    public void onNumbersPicked(int[] values) {
                        long postponeMillis = (values[0] * 24 + values[1]) *
                            3600L * 1000;
                        Date preferred = task.getPreferredDueDate();
                        if(preferred != null) {
                            preferred = new Date(preferred.getTime() +
                                    postponeMillis);
                            task.setPreferredDueDate(preferred);
                        }
                        Date definite = task.getDefiniteDueDate();
                        if(definite != null) {
                            definite = new Date(definite.getTime() +
                                    postponeMillis);
                            task.setDefiniteDueDate(definite);
                        }
                        getTaskController().saveTask(task);
                        fillData();
                    }
                });
            return true;

        case CONTEXT_FILTER_HIDDEN:
            TaskList.filterShowHidden = !filterShowHidden;
            saveTaskListSort();
            fillData();
            return true;
        case CONTEXT_FILTER_DONE:
            TaskList.filterShowDone = !filterShowDone;
            saveTaskListSort();
            fillData();
            return true;
        case CONTEXT_FILTER_TAG:
            TaskList.filterTag = null;
            fillData();
            return true;
        case CONTEXT_SORT_AUTO:
            if(sortMode == SortMode.AUTO)
                return true;
            TaskList.sortReverse = false;
            TaskList.sortMode = SortMode.AUTO;
            saveTaskListSort();
            fillData();
            return true;
        case CONTEXT_SORT_ALPHA:
            if(sortMode == SortMode.ALPHA)
                return true;
            TaskList.sortReverse = false;
            TaskList.sortMode = SortMode.ALPHA;
            saveTaskListSort();
            fillData();
            return true;
        case CONTEXT_SORT_DUEDATE:
            if(sortMode == SortMode.DUEDATE)
                return true;
            TaskList.sortReverse = false;
            TaskList.sortMode = SortMode.DUEDATE;
            saveTaskListSort();
            fillData();
            return true;
        case CONTEXT_SORT_REVERSE:
            TaskList.sortReverse = !sortReverse;
            saveTaskListSort();
            fillData();
            return true;
        }

        return false;
    }
}