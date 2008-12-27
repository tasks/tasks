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
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
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
import com.timsu.astrid.activities.TaskListAdapter.TaskListAdapterHooks;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForList;


/** Primary view for the Astrid Application. Lists all of the tasks in the
 * system, and allows users to edit them.
 *
 * @author Tim Su (timsu@stanfordalumni.org)
 *
 */
public class TaskList extends Activity {

    // bundle tokens
    public static final String     TAG_TOKEN             = "tag";

    // activities
    private static final int       ACTIVITY_CREATE       = 0;
    private static final int       ACTIVITY_VIEW         = 1;
    private static final int       ACTIVITY_EDIT         = 2;
    private static final int       ACTIVITY_TAGS         = 3;

    // menu codes
    private static final int       INSERT_ID             = Menu.FIRST;
    private static final int       FILTERS_ID            = Menu.FIRST + 1;
    private static final int       TAGS_ID               = Menu.FIRST + 2;
    private static final int       CONTEXT_FILTER_HIDDEN = Menu.FIRST + 20;
    private static final int       CONTEXT_FILTER_DONE   = Menu.FIRST + 21;

    // UI components
    private TaskController controller;
    private TagController tagController = null;
    private ListView listView;
    private Button addButton;

    // other instance variables
    private List<TaskModelForList> taskArray;
    private Map<TagIdentifier, TagModelForView> tagMap;
    private boolean filterShowHidden = false;
    private boolean filterShowDone = false;
    private TagModelForView filterTag = null;

    static boolean shouldCloseInstance = false;

    /** Called when loading up the activity for the first time */
    private void onLoad() {
        shouldCloseInstance = false;

        controller = new TaskController(this);
        controller.open();
        tagController = new TagController(this);
        tagController.open();

        listView = (ListView)findViewById(R.id.tasklist);
        addButton = (Button)findViewById(R.id.addtask);
        addButton.setOnClickListener(new
                View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createTask();
            }
        });

        fillData();

        // filters context menu
        listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                if(menu.hasVisibleItems())
                    return;

                MenuItem item = menu.add(Menu.NONE, CONTEXT_FILTER_HIDDEN, Menu.NONE,
                        R.string.taskList_filter_hidden);
                item.setCheckable(true);
                item.setChecked(filterShowHidden);

                item = menu.add(Menu.NONE, CONTEXT_FILTER_DONE, Menu.NONE,
                        R.string.taskList_filter_done);
                item.setCheckable(true);
                item.setChecked(filterShowDone);

                menu.setHeaderTitle(R.string.taskList_filter_title);
            }
        });
    }

    /** Fill in the Task List with our tasks */
    private void fillData() {
        Resources r = getResources();

        Cursor tasksCursor;

        // load tags (again)
        tagMap = tagController.getAllTagsAsMap(this);
        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey(TAG_TOKEN)) {
            TagIdentifier identifier = new TagIdentifier(extras.getLong(TAG_TOKEN));
            filterTag = tagMap.get(identifier);
        }

        // get the array of tasks
        if(filterTag != null) {
            List<TaskIdentifier> tasks = tagController.getTaggedTasks(this,
                    filterTag.getTagIdentifier());

            tasksCursor = controller.getTaskListCursorById(tasks);
        } else {
            if(filterShowDone)
                tasksCursor = controller.getAllTaskListCursor();
            else
                tasksCursor = controller.getActiveTaskListCursor();
        }

        startManagingCursor(tasksCursor);
        taskArray = controller.createTaskListFromCursor(tasksCursor,
                !filterShowHidden);
        int hiddenTasks = tasksCursor.getCount() - taskArray.size();

        // hide "add" button if we have a few tasks
        if(taskArray.size() > 2)
            addButton.setVisibility(View.GONE);

        // set up the title
        StringBuilder title = new StringBuilder().
            append(r.getString(R.string.taskList_titlePrefix)).append(" ");
        if(filterTag != null) {
            title.append(r.getString(R.string.taskList_titleTagPrefix,
                    filterTag.getName())).append(" ");
        }
        title.append(r.getQuantityString(R.plurals.Ntasks,
                taskArray.size(), taskArray.size()));
        if(hiddenTasks > 0)
            title.append(" (+").append(hiddenTasks).append(" ").
            append(r.getString(R.string.taskList_hiddenSuffix)).append(")");
        setTitle(title);

        // set up our adapter
        TaskListAdapter tasks = new TaskListAdapter(this, this,
                    R.layout.task_list_row, taskArray, new TaskListAdapterHooks() {
                @Override
                public TagController getTagController() {
                    return tagController;
                }

                @Override
                public Map<TagIdentifier, TagModelForView> getTagMap() {
                    return tagMap;
                }

                @Override
                public List<TaskModelForList> getTaskArray() {
                    return taskArray;
                }

                @Override
                public TaskController getTaskController() {
                    return controller;
                }

                @Override
                public void performItemClick(View v, int position) {
                    listView.performItemClick(v, position, 0);
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

                Intent intent = new Intent(TaskList.this, TaskView.class);
                intent.putExtra(TaskEdit.LOAD_INSTANCE_TOKEN, task.
                        getTaskIdentifier().getId());
                startActivityForResult(intent, ACTIVITY_VIEW);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {

        if(resultCode == TaskView.RESULT_DISMISS) {
            finish();
            return;
        }

        fillData();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus) { // stuff might have changed...
            if(shouldCloseInstance) // user wants to quit
                finish();
            else
                fillData();
            shouldCloseInstance = false;
        }
    }

    // --- list adapter

    private void createTask() {
        Intent intent = new Intent(this, TaskEdit.class);
        if(filterTag != null)
            intent.putExtra(TaskEdit.TAG_NAME_TOKEN, filterTag.getName());
        startActivityForResult(intent, ACTIVITY_CREATE);
    }

    private void deleteTask(final TaskIdentifier taskId) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_this_task_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    controller.deleteTask(taskId);
                    fillData();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        Intent intent;
        TaskModelForList task;

        switch(item.getItemId()) {
        case INSERT_ID:
            createTask();
            return true;
        case FILTERS_ID:
            listView.showContextMenu();
            return true;
        case TAGS_ID:
            if(filterTag == null) {
                intent = new Intent(TaskList.this, TagList.class);
                startActivityForResult(intent, ACTIVITY_TAGS);
            } else {
                finish();
            }
            return true;

        case TaskListAdapter.CONTEXT_EDIT_ID:
            task = taskArray.get(item.getGroupId());
            intent = new Intent(TaskList.this, TaskEdit.class);
            intent.putExtra(TaskEdit.LOAD_INSTANCE_TOKEN, task.getTaskIdentifier().getId());
            startActivityForResult(intent, ACTIVITY_EDIT);
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
            controller.saveTask(task);
            fillData();
            return true;

        case CONTEXT_FILTER_HIDDEN:
            filterShowHidden = !filterShowHidden;
            fillData();
            return true;
        case CONTEXT_FILTER_DONE:
            filterShowDone = !filterShowDone;
            fillData();
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    // --- creating stuff

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_list);

        onLoad();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem item;

        item = menu.add(Menu.NONE, INSERT_ID, Menu.NONE,
                R.string.taskList_menu_insert);
        item.setIcon(android.R.drawable.ic_menu_add);
        item.setAlphabeticShortcut('n');

        if(filterTag == null) {
            item = menu.add(Menu.NONE, FILTERS_ID, Menu.NONE,
                    R.string.taskList_menu_filters);
            item.setIcon(android.R.drawable.ic_menu_view);
            item.setAlphabeticShortcut('f');

            item = menu.add(Menu.NONE, TAGS_ID, Menu.NONE,
                    R.string.taskList_menu_tags);
            item.setIcon(android.R.drawable.ic_menu_myplaces);
            item.setAlphabeticShortcut('t');
        }

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controller.close();
        if(tagController != null)
            tagController.close();
    }
}