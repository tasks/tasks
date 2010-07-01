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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.StaleDataException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.timsu.astrid.activities.TaskListAdapter.TaskListAdapterHooks;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForEdit;
import com.timsu.astrid.data.task.TaskModelForList;
import com.timsu.astrid.sync.SynchronizationService;
import com.timsu.astrid.sync.Synchronizer;
import com.timsu.astrid.sync.Synchronizer.SynchronizerListener;
import com.timsu.astrid.utilities.AstridUtilities;
import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.DialogUtilities;
import com.timsu.astrid.utilities.Notifications;
import com.timsu.astrid.utilities.Preferences;
import com.timsu.astrid.utilities.TasksXmlExporter;
import com.timsu.astrid.utilities.TasksXmlImporter;
import com.timsu.astrid.widget.FilePickerBuilder;
import com.timsu.astrid.widget.NumberPicker;
import com.timsu.astrid.widget.NumberPickerDialog;
import com.timsu.astrid.widget.NNumberPickerDialog.OnNNumberPickedListener;
import com.timsu.astrid.widget.NumberPickerDialog.OnNumberPickedListener;
import com.todoroo.astrid.activity.TaskEditActivity;

/**
 * Primary view for the Astrid Application. Lists all of the tasks in the
 * system, and allows users to interact with them.
 *
 * @author timsu
 *
 */
public class TaskListSubActivity extends SubActivity {

    // bundle tokens
    public static final String TAG_TOKEN                  = "tag";
    public static final String FROM_NOTIFICATION_TOKEN    = "notify";
    public static final String NOTIF_FLAGS_TOKEN          = "notif_flags";
    public static final String NOTIF_REPEAT_TOKEN         = "notif_repeat";
    public static final String LOAD_INSTANCE_TOKEN        = "id";

    // activities
    private static final int   ACTIVITY_CREATE            = 0;
    private static final int   ACTIVITY_EDIT              = 1;
    private static final int   ACTIVITY_TAGS              = 2;
    private static final int   ACTIVITY_PREFERENCES       = 3;
    private static final int   ACTIVITY_SYNCHRONIZE       = 4;

    // menu codes
    private static final int   INSERT_ID                  = Menu.FIRST;
    private static final int   FILTERS_ID                 = Menu.FIRST + 1;
    private static final int   TAGS_ID                    = Menu.FIRST + 2;
    private static final int   SYNC_ID                    = Menu.FIRST + 3;
    private static final int   MORE_ID                    = Menu.FIRST + 4;

    private static final int   OPTIONS_SYNC_ID            = Menu.FIRST + 10;
    private static final int   OPTIONS_SETTINGS_ID        = Menu.FIRST + 11;
    private static final int   OPTIONS_HELP_ID            = Menu.FIRST + 12;
    private static final int   OPTIONS_CLEANUP_ID         = Menu.FIRST + 13;
    private static final int   OPTIONS_QUICK_TIPS         = Menu.FIRST + 14;
    private static final int   OPTIONS_EXPORT             = Menu.FIRST + 15;
    private static final int   OPTIONS_IMPORT             = Menu.FIRST + 16;

    private static final int   CONTEXT_FILTER_HIDDEN      = Menu.FIRST + 20;
    private static final int   CONTEXT_FILTER_DONE        = Menu.FIRST + 21;
    private static final int   CONTEXT_FILTER_TAG         = Menu.FIRST + 22;
    private static final int   CONTEXT_SORT_AUTO          = Menu.FIRST + 23;
    private static final int   CONTEXT_SORT_ALPHA         = Menu.FIRST + 24;
    private static final int   CONTEXT_SORT_DUEDATE       = Menu.FIRST + 25;
    private static final int   CONTEXT_SORT_REVERSE       = Menu.FIRST + 26;
    private static final int   CONTEXT_SORT_GROUP         = Menu.FIRST;

    // other constants
    private static final int   SORTFLAG_FILTERDONE        = (1 << 5);
    private static final int   SORTFLAG_FILTERHIDDEN      = (1 << 6);
    private static final float POSTPONE_STAT_PCT          = 0.4f;
    private static final int   AUTO_REFRESH_MAX_LIST_SIZE = 50;

    // UI components
    private ListView           listView;
    private ImageButton        addButton;
    private View               layout;
    private TextView           loadingText;

    // indicator flag set if task list should be refreshed (something changed
    // in another activity)
    public static boolean      shouldRefreshTaskList      = false;

    // indicator flag set if synchronization window has been opened & closed
    static boolean             syncPreferencesOpened      = false;

    // other instance variables
    static class TaskListContext {
        HashMap<TagIdentifier, TagModelForView> tagMap;
        List<TaskModelForList>                  taskArray;
        HashMap<Long, TaskModelForList>         tasksById;
        HashMap<TaskModelForList, String>       taskTags;
        TaskModelForList                        selectedTask  = null;
        Thread                                  loadingThread = null;
        TaskListAdapter                         listAdapter   = null;
        TagModelForView                         filterTag     = null;
        CharSequence                            windowTitle;
    }

    Handler                 handler          = null;
    Long                    selectedTaskId   = null;
    Runnable                reLoadRunnable   = null;
    private TaskListContext context;

    // display filters
    private static boolean  filterShowHidden = false;
    private static boolean  filterShowDone   = false;
    private static SortMode sortMode         = SortMode.AUTO;
    private static boolean  sortReverse      = false;

    /*
     * ======================================================================
     * ======================================================= initialization
     * ======================================================================
     */

    public TaskListSubActivity(TaskList parent, int code, View view) {
        super(parent, code, view);
    }

    @Override
    /*  Called when loading up the activity */
    public void onDisplay(final Bundle variables) {
        // process task that's selected, if any
        if (variables != null && variables.containsKey(LOAD_INSTANCE_TOKEN)) {
            selectedTaskId = variables.getLong(LOAD_INSTANCE_TOKEN);
        } else {
            selectedTaskId = null;
        }
        setupUIComponents();

        // declare the reload runnable, which is called when the task list
        // wants to reload itself
        reLoadRunnable = new Runnable() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        loadingText.setText(getParent().getResources()
                                .getString(R.string.updating));
                    }
                });

                fillData();
            }
        };

        // if we have a non-configuration instance (i.e. the screen was just
        // rotated), use that instead of loading the whole task list again.
        // this makes screen rotation an inexpensive operation
        if (getLastNonConfigurationInstance() != null) {
            context = (TaskListContext) getLastNonConfigurationInstance();
            listView.setAdapter(context.listAdapter);
            onTaskListLoaded();
            return;
        }

        context = new TaskListContext();
        if (selectedTaskId == null)
            context.selectedTask = null;

        // process tag to filter, if any (intercept UNTAGGED identifier, if
        // applicable)
        if (variables != null && variables.containsKey(TAG_TOKEN)) {
            TagIdentifier identifier = new TagIdentifier(variables
                    .getLong(TAG_TOKEN));
            context.tagMap = getTagController().getAllTagsAsMap();
            if (context.tagMap.containsKey(identifier))
                context.filterTag = context.tagMap.get(identifier);
            else if (identifier.equals(TagModelForView.UNTAGGED_IDENTIFIER))
                context.filterTag = TagModelForView.getUntaggedModel();
            else
                Toast.makeText(getParent(), R.string.missing_tag,
                        Toast.LENGTH_SHORT).show();

            FlurryAgent.onEvent("filter-by-tag");
        }

        // time to go! creates a thread that loads the task list, then
        // displays the reminder box if it is requested
        context.loadingThread = new Thread(new Runnable() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        loadingText.setVisibility(View.VISIBLE);
                    }
                });

                loadTaskListSort();
                fillData();

                // open up reminder box
                if (variables != null
                    && variables.containsKey(NOTIF_FLAGS_TOKEN)
                    && context.selectedTask != null) {
                    FlurryAgent.onEvent("open-notification");
                    handler.post(new Runnable() {
                        public void run() {
                            long repeatInterval = 0;
                            int flags = 0;

                            if (variables.containsKey(NOTIF_REPEAT_TOKEN))
                                repeatInterval = variables
                                        .getLong(NOTIF_REPEAT_TOKEN);
                            flags = variables.getInt(NOTIF_FLAGS_TOKEN);
                            showNotificationAlert(context.selectedTask,
                                    repeatInterval, flags);
                        }
                    });
                }
            }
        });
        context.loadingThread.start();
    }

    /** Initialize UI components */
    public void setupUIComponents() {
        handler = new Handler();

        listView = (ListView) findViewById(R.id.tasklist);
        loadingText = (TextView) findViewById(R.id.loading);
        addButton = (ImageButton) findViewById(R.id.quickAddButton);
        addButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView quickAdd = (TextView)findViewById(R.id.quickAddText);
                if(quickAdd.getText().length() > 0) {
                    quickAddTask(quickAdd.getText().toString());
                    quickAdd.setText(""); //$NON-NLS-1$
                    reloadList();
                } else {
                    createTask(null);
                }
            }
        });

        layout = getView();
        layout.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                if (menu.hasVisibleItems())
                    return;
                onCreateMoreOptionsMenu(menu);
            }
        });

        // disable quick-add keyboard until user requests it
        EditText quickAdd = (EditText)findViewById(R.id.quickAddText);
        AstridUtilities.suppressVirtualKeyboard(quickAdd);
    }

    /**
     * Quick-add a new task
     * @param title
     * @return
     */
    @SuppressWarnings("nls")
    protected TaskModelForEdit quickAddTask(String title) {
        TaskModelForEdit task = getTaskController().createNewTaskForEdit();
        try {
            task.setName(title);
            getTaskController().saveTask(task, false);
            if (context.filterTag != null) {
                getTagController().addTag(task.getTaskIdentifier(),
                        context.filterTag.getTagIdentifier());
            }
            return task;
        } catch (Exception e) {
            AstridUtilities.reportFlurryError("quick-add-task", e);
            return task;
        }

    }

    @Override
    /*  Create options menu (displayed when user presses menu key) */
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

        if (Constants.SYNCHRONIZE && Preferences.shouldDisplaySyncButton(getParent())) {
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

    /** Create 'more options' menu */
    public boolean onCreateMoreOptionsMenu(Menu menu) {
        MenuItem item;

        if(Constants.SYNCHRONIZE) {
            item = menu.add(Menu.NONE, OPTIONS_SYNC_ID, Menu.NONE,
                    R.string.taskList_menu_sync);
            item.setAlphabeticShortcut('s');
        }

        item = menu.add(Menu.NONE, OPTIONS_SETTINGS_ID, Menu.NONE,
                R.string.taskList_menu_settings);
        item.setAlphabeticShortcut('p');

        item = menu.add(Menu.NONE, OPTIONS_CLEANUP_ID, Menu.NONE,
                R.string.taskList_menu_cleanup);

        item = menu.add(Menu.NONE, OPTIONS_QUICK_TIPS, Menu.NONE,
                R.string.taskList_menu_tips);

        item = menu.add(Menu.NONE, OPTIONS_EXPORT, Menu.NONE,
                R.string.taskList_menu_export);

        item = menu.add(Menu.NONE, OPTIONS_IMPORT, Menu.NONE,
                R.string.taskList_menu_import);

        item = menu.add(Menu.NONE, OPTIONS_HELP_ID, Menu.NONE,
                R.string.taskList_menu_help);
        item.setAlphabeticShortcut('h');

        return true;
    }

    /**
     * Enum that determines how the task list is sorted. Contains a comparison
     * method that determines sorting order.
     *
     * @author timsu
     *
     */
    private enum SortMode {
        ALPHA {
            @Override
            int compareTo(TaskModelForList arg0, TaskModelForList arg1) {
                return arg0.getName().toLowerCase().compareTo(
                        arg1.getName().toLowerCase());
            }
        },
        DUEDATE {
            long getDueDate(TaskModelForList task) {
                Date definite = task.getDefiniteDueDate();
                Date preferred = task.getPreferredDueDate();
                if (definite != null && preferred != null) {
                    if (preferred.getTime() < System.currentTimeMillis())
                        return definite.getTime();
                    return preferred.getTime();
                } else if (definite != null)
                    return definite.getTime();
                else if (preferred != null)
                    return preferred.getTime();
                else
                    return new Date(2020, 1, 1).getTime();
            }

            @Override
            int compareTo(TaskModelForList arg0, TaskModelForList arg1) {
                return (int) ((getDueDate(arg0) - getDueDate(arg1)) / 1000);
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

    /*
     * ======================================================================
     * ======================================================== notifications
     * ======================================================================
     */

    /** Called when user clicks on a notification to get here */
    private void showNotificationAlert(final TaskModelForList task,
            final long repeatInterval, final int flags) {
        Resources r = getResources();

        // clear notifications
        Notifications.clearAllNotifications(getParent(), task
                .getTaskIdentifier());

        String[] strings = new String[] {
            r.getString(R.string.notify_yes),
            r.getString(R.string.notify_done),
            r.getString(R.string.notify_snooze),
            r.getString(R.string.notify_no)
        };

        String response;
        if (Preferences.shouldShowNags(getParent())) {
            String[] responses = r.getStringArray(R.array.reminder_responses);
            response = responses[new Random().nextInt(responses.length)];
        } else
            response = r.getString(R.string.taskList_nonag_reminder);

        AlertDialog.Builder builder = new AlertDialog.Builder(getParent());
        final AlertDialog dialog;

        LayoutInflater inflater = (LayoutInflater) getParent().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.notification_dialog, null);

        builder.setTitle(task.getName());
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setCancelable(true);
        builder.setView(dialogView);
        dialog = builder.create();

        TextView message = (TextView)dialogView.findViewById(R.id.message);
        message.setText(response);
        message.setTextSize(18);

        ListView items = (ListView)dialogView.findViewById(R.id.items);
        items.setAdapter(new ArrayAdapter<String>(getParent(),
                android.R.layout.simple_list_item_checked, strings));
        items.setFocusableInTouchMode(true);
        items.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int which,
                    long id) {
                switch(which) {
                case 0:
                    break;
                case 1:
                    task.setProgressPercentage(TaskModelForList.COMPLETE_PERCENTAGE);
                    getTaskController().saveTask(task, false);
                    break;
                case 2:
                    snoozeAlert(task, repeatInterval, flags);
                    break;
                case 3:
                    TaskList.shouldCloseInstance = true;
                    closeActivity();
                    break;
                }
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Helper method to "snooze" an alert (i.e. set a new one for some time from
     * now.
     *
     * @param task
     * @param repeatInterval
     * @param flags
     */
    private void snoozeAlert(final TaskModelForList task,
            final long repeatInterval, final int flags) {
        DialogUtilities.hourMinutePicker(getParent(), getResources().getString(
                R.string.notify_snooze_title), new OnNNumberPickedListener() {
            public void onNumbersPicked(int[] values) {
                int snoozeSeconds = values[0] * 3600 + values[1] * 60;
                Notifications.createSnoozeAlarm(getParent(), task
                        .getTaskIdentifier(), snoozeSeconds, flags,
                        repeatInterval);

                TaskList.shouldCloseInstance = true;
                closeActivity();
            }
        });
    }

    /*
     * ======================================================================
     * ====================================================== populating list
     * ======================================================================
     */

    /** Helper method returns true if the task is considered 'hidden' */
    private boolean isTaskHidden(TaskModelForList task) {
        if (task == context.selectedTask)
            return false;

        if (task.isHidden())
            return true;

        if (context.filterTag == null) {
            if (context.taskTags.get(task).contains(
                    TagModelForView.HIDDEN_FROM_MAIN_LIST_PREFIX))
                return true;
        }

        return false;
    }

    /** Fill in the Task List with our tasks */
    private synchronized void fillData() {
        int hiddenTasks = 0; // # of tasks hidden
        int completedTasks = 0; // # of tasks on list that are done

        handler.post(new Runnable() {
            public void run() {
                loadingText.setVisibility(View.VISIBLE);
            }
        });

        try {
            // get a cursor to the task list
            Cursor tasksCursor;
            if (context.filterTag != null) { // Filter by TAG
                LinkedList<TaskIdentifier> tasks;

                // Check "named" Tag vs. "Untagged"
                TagIdentifier tagId = context.filterTag.getTagIdentifier();
                if (!tagId.equals(TagModelForView.UNTAGGED_IDENTIFIER)) {
                    tasks = getTagController().getTaggedTasks(tagId);
                } else {
                    tasks = getTagController().getUntaggedTasks();
                }
                tasksCursor = getTaskController().getTaskListCursorById(tasks);

            } else {
                if (filterShowDone)
                    tasksCursor = getTaskController().getAllTaskListCursor();
                else
                    tasksCursor = getTaskController().getActiveTaskListCursor();
            }
            // if internal state is compromised, bail out
            if(tasksCursor == null)
                return;
            startManagingCursor(tasksCursor);
            context.taskArray = Collections
                    .synchronizedList(getTaskController()
                            .createTaskListFromCursor(tasksCursor));

            // read tags and apply filters
            context.tagMap = getTagController().getAllTagsAsMap();
            context.taskTags = new HashMap<TaskModelForList, String>();
            StringBuilder tagBuilder = new StringBuilder();
            context.tasksById = new HashMap<Long, TaskModelForList>();

            // null may occur when extremely low memory(?)
            // tsu: i'm not sure why, but we get NPE's from the for loop
            if(context.taskArray == null)
                return;

            for (Iterator<TaskModelForList> i = context.taskArray.iterator(); i
                    .hasNext();) {
                if (Thread.interrupted())
                    return;

                final TaskModelForList task = i.next();

                if (!filterShowDone) {
                    if (task.isTaskCompleted()) {
                        i.remove();
                        continue;
                    }
                }

                if (selectedTaskId != null
                    && task.getTaskIdentifier().getId() == selectedTaskId) {
                    context.selectedTask = task;
                }

                // get list of tags
                LinkedList<TagIdentifier> tagIds = getTagController()
                        .getTaskTags(task.getTaskIdentifier());
                tagBuilder.delete(0, tagBuilder.length());
                for (Iterator<TagIdentifier> j = tagIds.iterator(); j.hasNext();) {
                    TagIdentifier id = j.next();
                    if(!context.tagMap.containsKey(id)) {
                        // tag identifier does not exist anymore
                        Log.w("task-list", "WARNING: tag id was deleted - " + id);
                        getTagController().removeTag(task.getTaskIdentifier(), id);
                        continue;
                    }

                    TagModelForView tag = context.tagMap.get(id);

                    // bad entry from map (can this ever happen? at least we
                    // won't crash
                    if(tag == null) {
                        Log.w("task-list", "WARNING: tag id was null: " + id);
                        continue;
                    }

                    tagBuilder.append(tag.getName());
                    if (j.hasNext())
                        tagBuilder.append(", ");
                }
                context.taskTags.put(task, tagBuilder.toString());

                // hide hidden
                if (!filterShowHidden) {
                    if (isTaskHidden(task)) {
                        hiddenTasks++;
                        i.remove();
                        continue;
                    }
                }

                context.tasksById.put(task.getTaskIdentifier().getId(), task);

                if (task.isTaskCompleted())
                    completedTasks++;
            }

            HashMap<String, String> args = new HashMap<String, String>();
            args.put("tasks", Integer.toString(context.taskArray.size()));
            FlurryAgent.onEvent("loaded-tasks", args);

        } catch (StaleDataException e) {
            // happens when you rotate the screen while the thread is
            // still running. i don't think it's avoidable?
            Log.w("astrid", "StaleDataException", e);
            return;
        } catch (final IllegalStateException e) {

            // activity has been closed. suppress error
            if(e.getMessage().contains("attempt to acquire a reference on a close SQLiteClosable")) {
                Log.w("astrid", "Caught error", e);
                AstridUtilities.reportFlurryError("task-list-error-caught", e);


            // may happen when you run out of memory usually
            } else {
                AstridUtilities.reportFlurryError("task-list-error", e);
                Log.e("astrid", "Error loading task list", e);
                handler.post(new Runnable() {
                    public void run() {
                        if (!e.getMessage().contains("Couldn't init cursor window"))
                            return;
                        DialogUtilities.okDialog(getParent(), "Ran out of memory! "
                            + "Try restarting Astrid...", null);
                    }
                });
                return;
            }
        } catch (SQLiteException e) {
            // log it but don't throw it
            Log.e("astrid", "Error loading task list", e);
            // DialogUtilities.okDialog(getParent(), "Error loading task list: " + e.getMessage() + ".\n\nOffending line: " + e.getStackTrace()[0], null);

        } catch (final Exception e) {
            AstridUtilities.reportFlurryError("task-list-error", e);
            Log.e("astrid", "Error loading task list", e);
            DialogUtilities.okDialog(getParent(), "Error loading task list: " + e.getMessage() + ".\n\nOffending line: " + e.getStackTrace()[0], null);
            onTaskListLoaded();
            return;
        }

        try {
            int activeTasks = context.taskArray.size() - completedTasks;

            // sort task list
            Collections.sort(context.taskArray, new Comparator<TaskModelForList>() {
                public int compare(TaskModelForList arg0, TaskModelForList arg1) {
                    return sortMode.compareTo(arg0, arg1);
                }
            });
            if (sortReverse)
                Collections.reverse(context.taskArray);

            final int finalCompleted = completedTasks;
            final int finalActive = activeTasks;
            final int finalHidden = hiddenTasks;

            handler.post(new Runnable() {
                public void run() {
                    Resources r = getResources();
                    StringBuilder title = new StringBuilder().append(
                        r.getString(R.string.taskList_titlePrefix)).append(" ");
                    if (context.filterTag != null) {
                        if (TagModelForView.UNTAGGED_IDENTIFIER.equals(context.filterTag.getTagIdentifier())) {
                            title.append(
                                r.getString(R.string.taskList_titleUntagged)).append(
                                " ");
                        } else {
                            title.append(
                                r.getString(R.string.taskList_titleTagPrefix,
                                    context.filterTag.getName())).append(" ");
                        }
                    }

                    if (finalCompleted > 0)
                        title.append(r.getQuantityString(R.plurals.NactiveTasks,
                            finalActive, finalActive, context.taskArray.size()));
                    else
                        title.append(r.getQuantityString(R.plurals.Ntasks,
                            context.taskArray.size(), context.taskArray.size()));
                    if (finalHidden > 0)
                        title.append(" (+").append(finalHidden).append(" ").append(
                            r.getString(R.string.taskList_hiddenSuffix)).append(")");
                    context.windowTitle = title;
                }
            });
        } catch (Exception e) {
            AstridUtilities.reportFlurryError("task-list-error-block2", e);
            Log.e("astrid", "Error loading task list block 2", e);
        }

        onTaskListLoaded();
    }

    /** Sets up the interface after everything has been loaded */
    private void onTaskListLoaded() {
        // set up the title
        handler.post(new Runnable() {
            public void run() {
                setTitle(context.windowTitle);
                setUpListUI();
                loadingText.setVisibility(View.GONE);
            }
        });
    }

    class TaskListHooks implements TaskListAdapterHooks {

        private final HashMap<TaskModelForList, String> myTaskTags;
        private final List<TaskModelForList>            myTaskArray;

        public TaskListHooks() {
            this.myTaskTags = context.taskTags;
            this.myTaskArray = context.taskArray;
        }

        public TagController tagController() {
            return getTagController();
        }

        public String getTagsFor(TaskModelForList task) {
            return myTaskTags.get(task);
        }

        public List<TaskModelForList> getTaskArray() {
            return myTaskArray;
        }

        public TaskController taskController() {
            return getTaskController();
        }

        public void performItemClick(View v, int position) {
            listView.performItemClick(v, position, 0);
        }

        public void onCreatedTaskListView(View v, TaskModelForList task) {
            v.setOnTouchListener(getGestureListener());
        }

        public void editItem(TaskModelForList task) {
            editTask(task);
        }

        public void toggleTimerOnItem(TaskModelForList task) {
            toggleTimer(task);
        }

        public void setSelectedItem(TaskIdentifier taskId) {
            if (taskId == null) {
                selectedTaskId = null;
                context.selectedTask = null;
            } else
                selectedTaskId = taskId.getId();
        }
    }

    /** Set up the adapter for our task list */
    private void setUpListUI() {
        // if something happened, don't finish setting up the list
        if(context.taskArray == null)
            return;

        // set up our adapter
        context.listAdapter = new TaskListAdapter(getParent(),
            R.layout.task_list_row, context.taskArray, new TaskListHooks());
        listView.setAdapter(context.listAdapter);
        listView.setItemsCanFocus(true);

        if (context.selectedTask != null) {
            try {
                int selectedPosition = context.listAdapter.getPosition(context.selectedTask);
                View v = listView.getChildAt(selectedPosition);
                context.listAdapter.setExpanded(v, context.selectedTask, true);
                listView.setSelection(selectedPosition);
            } catch (Exception e) {
                AstridUtilities.reportFlurryError("task-list-selected", e);
                Log.e("astrid", "error with selected task", e);
            }
        }

        // filters context menu
        listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                if (menu.hasVisibleItems())
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

                if (context.filterTag != null) {
                    item = menu.add(Menu.NONE, CONTEXT_FILTER_TAG, Menu.NONE,
                            r.getString(R.string.taskList_filter_tagged,
                                    context.filterTag.getName()));
                    item.setCheckable(true);
                    item.setChecked(true);
                }

                item = menu.add(CONTEXT_SORT_GROUP, CONTEXT_SORT_AUTO,
                        Menu.NONE, R.string.taskList_sort_auto);
                item.setChecked(sortMode == SortMode.AUTO);
                item = menu.add(CONTEXT_SORT_GROUP, CONTEXT_SORT_ALPHA,
                        Menu.NONE, R.string.taskList_sort_alpha);
                item.setChecked(sortMode == SortMode.ALPHA);
                item = menu.add(CONTEXT_SORT_GROUP, CONTEXT_SORT_DUEDATE,
                        Menu.NONE, R.string.taskList_sort_duedate);
                item.setChecked(sortMode == SortMode.DUEDATE);
                menu.setGroupCheckable(CONTEXT_SORT_GROUP, true, true);

                item = menu.add(CONTEXT_SORT_GROUP, CONTEXT_SORT_REVERSE,
                        Menu.NONE, R.string.taskList_sort_reverse);
                item.setCheckable(true);
                item.setChecked(sortReverse);
            }
        });

        listView.setOnTouchListener(getGestureListener());
    }

    private void reloadList() {
        if (context.loadingThread != null && context.loadingThread.isAlive()) {
            context.loadingThread.interrupt();
        }
        context.loadingThread = new Thread(reLoadRunnable);
        context.loadingThread.start();
    }

    /*
     * ======================================================================
     * ======================================================= event handlers
     * ======================================================================
     */

    @Override
    protected Object onRetainNonConfigurationInstance() {
        return context;
    }

    @Override
    protected boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (context.filterTag != null) {
                showTagsView();
                return true;
            } else {
                // close the app
                getParent().finish();
            }
        }

        // if it's a printable character that's not 1..4
        char character = event.getNumber();
        if (character >= '!' && (character < '1' || character > '4') &&
                character <= '~') {
            createTask(event.getNumber());
            return true;
        }

        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (context.loadingThread != null && context.loadingThread.isAlive())
            context.loadingThread.stop();
    }

    @Override
    void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            if (shouldRefreshTaskList)
                reloadList();
            else if (syncPreferencesOpened) {
                syncPreferencesOpened = false;

                if (TaskList.synchronizeNow) {
                    TaskList.synchronizeNow = false;
                    synchronize();
                }

                // schedule synchronization service
                if(Constants.SYNCHRONIZE)
                    SynchronizationService.scheduleService(getParent());

            } else if (context.taskArray != null
                && context.taskArray.size() > 0
                && context.taskArray.size() < AUTO_REFRESH_MAX_LIST_SIZE) {

                // invalidate caches
                for (TaskModelForList task : context.taskArray)
                    task.clearCache();
                listView.invalidateViews();
            }
        }

        shouldRefreshTaskList = false;
    }

    /** Invoke synchronizer */
    private void synchronize() {
        if(!Constants.SYNCHRONIZE)
            return;

        Synchronizer sync = new Synchronizer(false);
        sync.setTagController(getTagController());
        sync.setTaskController(getTaskController());
        sync.synchronize(getParent(), new SynchronizerListener() {
            public void onSynchronizerFinished(int numServicesSynced) {
                if (numServicesSynced == 0) {
                    DialogUtilities.okDialog(getParent(), getResources()
                            .getString(R.string.sync_no_synchronizers), null);
                    return;
                }
                reloadList();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Constants.RESULT_SYNCHRONIZE) {
            synchronize();
        } else if (requestCode == ACTIVITY_TAGS) {
            switchToActivity(TaskList.AC_TAG_LIST, null);
        } else if(requestCode == ACTIVITY_PREFERENCES) {
            reloadList();
        }
    }

    /** Call an activity to create the given task */
    private void createTask(Character startCharacter) {
        Intent intent = new Intent(getParent(), TaskEditActivity.class);
        launchActivity(intent, ACTIVITY_CREATE);
    }

    /** Show a dialog box and delete the task specified */
    private void deleteTask(final TaskModelForList task) {
        new AlertDialog.Builder(getParent()).setTitle(R.string.delete_title)
                .setMessage(R.string.delete_this_task_title).setIcon(
                        android.R.drawable.ic_dialog_alert).setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                context.listAdapter.remove(task);
                                context.taskArray.remove(task);
                                getTaskController().deleteTask(
                                        task.getTaskIdentifier());
                            }
                        }).setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Take you to the task edit page */
    private void editTask(TaskModelForList task) {
        Intent intent = new Intent(getParent(), TaskEditActivity.class);
        intent.putExtra(TaskEditActivity.ID_TOKEN, task.getTaskIdentifier()
                .getId());
        launchActivity(intent, ACTIVITY_EDIT);
    }

    /** Toggle the timer */
    private void toggleTimer(TaskModelForList task) {
        if(task == null)
            return;

        if (task.getTimerStart() == null) {
            FlurryAgent.onEvent("start-timer");
            task.setTimerStart(new Date());
        } else {
            FlurryAgent.onEvent("stop-timer");
            task.stopTimerAndUpdateElapsedTime();
        }
        getTaskController().saveTask(task, false);
        context.listAdapter.refreshItem(listView, context.taskArray
                .indexOf(task));
    }

    /** Show the tags view */
    public void showTagsView() {
        switchToActivity(TaskList.AC_TAG_LIST, null);
    }

    @Override
    public void launchActivity(Intent intent, int requestCode) {
        super.launchActivity(intent, requestCode);
    }

    /** Save the sorting mode to the preferences */
    private void saveTaskListSort() {
        int sortId = sortMode.ordinal() + 1;

        if (filterShowDone)
            sortId |= SORTFLAG_FILTERDONE;
        if (filterShowHidden)
            sortId |= SORTFLAG_FILTERHIDDEN;

        if (sortReverse)
            sortId *= -1;

        Preferences.setTaskListSort(getParent(), sortId);
    }

    /** Save the sorting mode to the preferences */
    private void loadTaskListSort() {
        int sortId = Preferences.getTaskListSort(getParent());
        if (sortId == 0)
            return;
        sortReverse = sortId < 0;
        sortId = Math.abs(sortId);

        filterShowDone = (sortId & SORTFLAG_FILTERDONE) > 0;
        filterShowHidden = (sortId & SORTFLAG_FILTERHIDDEN) > 0;

        sortId = sortId & ~(SORTFLAG_FILTERDONE | SORTFLAG_FILTERHIDDEN);

        sortMode = SortMode.values()[sortId - 1];
    }

    /** Compute date after postponing tasks */
    private Date computePostponeDate(Date input, long postponeMillis,
            boolean shiftFromTodayIfPast) {
        if (input != null) {
            if (shiftFromTodayIfPast
                && input.getTime() < System.currentTimeMillis())
                input = new Date();
            input = new Date(input.getTime() + postponeMillis);
        }

        return input;
    }

    /** Show a dialog box and delete old tasks as requested */
    private void cleanOldTasks() {
        final Resources r = getResources();
        new NumberPickerDialog(getParent(), new OnNumberPickedListener() {
            public void onNumberPicked(NumberPicker view, int number) {
                Date date = new Date(System.currentTimeMillis() - 24L * 3600
                    * 1000 * number);
                int deleted = getTaskController()
                        .deleteCompletedTasksOlderThan(date);
                DialogUtilities.okDialog(getParent(), r.getQuantityString(
                        R.plurals.Ntasks, deleted, deleted)
                    + " " + r.getString(R.string.taskList_deleted), null);
                if (TaskListSubActivity.filterShowDone)
                    reloadList();
            }
        }, r.getString(R.string.taskList_cleanup_dialog), 30, 5, 0, 999).show();
    }

    /** Show a dialog box to postpone your tasks */
    private void postponeTask(final TaskModelForList task) {
        FlurryAgent.onEvent("postpone-task");

        final Resources r = getResources();
        DialogUtilities.dayHourPicker(getParent(), r
                .getString(R.string.taskList_postpone_dialog),
                new OnNNumberPickedListener() {
                    public void onNumbersPicked(int[] values) {
                        long postponeMillis = (values[0] * 24 + values[1]) * 3600L * 1000;
                        if (postponeMillis <= 0)
                            return;

                        task.setPreferredDueDate(computePostponeDate(task
                                .getPreferredDueDate(), postponeMillis, true));
                        task.setDefiniteDueDate(computePostponeDate(task
                                .getDefiniteDueDate(), postponeMillis, true));
                        task.setHiddenUntil(computePostponeDate(task
                                .getHiddenUntil(), postponeMillis, false));

                        // show nag
                        int postponeCount = getTaskController()
                                .fetchTaskPostponeCount(
                                        task.getTaskIdentifier()) + 1;
                        if (Preferences.shouldShowNags(getParent())) {
                            Random random = new Random();
                            final String nagText;
                            if (postponeCount > 1
                                && random.nextFloat() < POSTPONE_STAT_PCT) {
                                nagText = r.getString(
                                        R.string.taskList_postpone_count,
                                        postponeCount);
                            } else {
                                String[] nags = r
                                        .getStringArray(R.array.postpone_nags);
                                nagText = nags[random.nextInt(nags.length)];
                            }

                            handler.post(new Runnable() {
                                public void run() {
                                    Toast.makeText(getParent(), nagText,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        task.setPostponeCount(postponeCount);

                        getTaskController().saveTask(task, false);
                        getTaskController().updateAlarmForTask(
                                task.getTaskIdentifier());
                        context.listAdapter.refreshItem(listView,
                                context.taskArray.indexOf(task));
                    }
                });
    }

    @Override
    public boolean onMenuItemSelected(int featureId, final MenuItem item) {
        final TaskModelForList task;

        switch (item.getItemId()) {
        // --- options menu items
        case INSERT_ID:
            createTask(null);
            return true;
        case FILTERS_ID:
            listView.showContextMenu();
            return true;
        case TAGS_ID:
            showTagsView();
            return true;
        case SYNC_ID:
        	synchronize();
            return true;
        case MORE_ID:
            layout.showContextMenu();
            return true;

            // --- more options menu items
        case OPTIONS_SYNC_ID:
            syncPreferencesOpened = true;
            launchActivity(new Intent(getParent(), SyncPreferences.class),
                    ACTIVITY_SYNCHRONIZE);
            return true;
        case OPTIONS_SETTINGS_ID:
            launchActivity(new Intent(getParent(), EditPreferences.class),
                    ACTIVITY_PREFERENCES);
            return true;
        case OPTIONS_HELP_ID:
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
                    .parse(Constants.HELP_URL));
            launchActivity(browserIntent, 0);
            return true;
        case OPTIONS_QUICK_TIPS:
            DialogUtilities.okDialog(getParent(), getResources().getString(
                    R.string.quick_tips), null);
            return true;
        case OPTIONS_CLEANUP_ID:
            cleanOldTasks();
            return true;
        case OPTIONS_EXPORT:
            exportTasks();
            return true;
        case OPTIONS_IMPORT:
            importTasks();
            return true;

            // --- list context menu items
        case TaskListAdapter.CONTEXT_EDIT_ID:
            task = context.tasksById.get((long) item.getGroupId());
            editTask(task);
            return true;
        case TaskListAdapter.CONTEXT_DELETE_ID:
            task = context.tasksById.get((long) item.getGroupId());
            deleteTask(task);
            return true;
        case TaskListAdapter.CONTEXT_TIMER_ID:
            task = context.tasksById.get((long) item.getGroupId());
            toggleTimer(task);
            return true;
        case TaskListAdapter.CONTEXT_POSTPONE_ID:
            task = context.tasksById.get((long) item.getGroupId());
            postponeTask(task);
            return true;

            // --- display context menu items
        case CONTEXT_FILTER_HIDDEN:
            TaskListSubActivity.filterShowHidden = !filterShowHidden;
            saveTaskListSort();
            reloadList();
            return true;
        case CONTEXT_FILTER_DONE:
            TaskListSubActivity.filterShowDone = !filterShowDone;
            saveTaskListSort();
            reloadList();
            return true;
        case CONTEXT_FILTER_TAG:
            switchToActivity(TaskList.AC_TASK_LIST, null);
            return true;
        case CONTEXT_SORT_AUTO:
            if (sortMode == SortMode.AUTO)
                return true;
            TaskListSubActivity.sortReverse = false;
            TaskListSubActivity.sortMode = SortMode.AUTO;
            saveTaskListSort();
            reloadList();
            return true;
        case CONTEXT_SORT_ALPHA:
            if (sortMode == SortMode.ALPHA)
                return true;
            TaskListSubActivity.sortReverse = false;
            TaskListSubActivity.sortMode = SortMode.ALPHA;
            saveTaskListSort();
            reloadList();
            return true;
        case CONTEXT_SORT_DUEDATE:
            if (sortMode == SortMode.DUEDATE)
                return true;
            TaskListSubActivity.sortReverse = false;
            TaskListSubActivity.sortMode = SortMode.DUEDATE;
            saveTaskListSort();
            reloadList();
            return true;
        case CONTEXT_SORT_REVERSE:
            TaskListSubActivity.sortReverse = !sortReverse;
            saveTaskListSort();
            reloadList();
            return true;
        }

        return false;
    }

    private void importTasks() {
        final Runnable reloadList = new Runnable() {
            public void run() {
                reloadList();
            }
        };
        final Context ctx = this.getParent();
        FilePickerBuilder.OnFilePickedListener listener = new FilePickerBuilder.OnFilePickedListener() {
            @Override
            public void onFilePicked(String filePath) {
                TasksXmlImporter importer = new TasksXmlImporter(ctx);
                importer.setInput(filePath);
                importer.importTasks(reloadList);
            }
        };
        DialogUtilities.filePicker(ctx,
                ctx.getString(R.string.import_file_prompt),
                TasksXmlExporter.getExportDirectory(),
                listener);
    }

    private void exportTasks() {
        TasksXmlExporter exporter = new TasksXmlExporter(false);
        exporter.setContext(getParent());
        exporter.exportTasks(TasksXmlExporter.getExportDirectory());
    }

    /*
     * ======================================================================
     * ===================================================== getters / setters
     * ======================================================================
     */

    public TagModelForView getFilterTag() {
        return context.filterTag;
    }
}
