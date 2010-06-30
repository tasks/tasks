/*
 * ASTRID: Android's Simple Task Recording Dashboard
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

import java.text.Format;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.timsu.astrid.data.alerts.AlertController;
import com.timsu.astrid.data.enums.Importance;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForList;
import com.timsu.astrid.data.task.AbstractTaskModel.RepeatInfo;
import com.timsu.astrid.utilities.AstridUtilities;
import com.timsu.astrid.utilities.DateUtilities;
import com.timsu.astrid.utilities.Preferences;
import com.timsu.astrid.utilities.TaskFieldsVisibility;

/**
 * Adapter for displaying a list of TaskModelForList entities
 *
 * @author timsu
 *
 */
public class TaskListAdapter extends ArrayAdapter<TaskModelForList> {

    public static final int CONTEXT_EDIT_ID       = Menu.FIRST + 50;
    public static final int CONTEXT_DELETE_ID     = Menu.FIRST + 51;
    public static final int CONTEXT_TIMER_ID      = Menu.FIRST + 52;
    public static final int CONTEXT_POSTPONE_ID   = Menu.FIRST + 53;

    // keys for caching task properties
    private static final int KEY_NAME      = 0;
    private static final int KEY_DEADLINE  = 1;
    private static final int KEY_OVERDUE   = 2;
    private static final int KEY_REPEAT    = 3;
    private static final int KEY_REMINDERS = 4;
    private static final int KEY_TIMES     = 5;
    private static final int KEY_TAGS      = 6;
    private static final int KEY_HIDDEN    = 7;
    private static final int KEY_EXPANDED  = 8;
    private static final int KEY_CREATION  = 9;

    private static final String CACHE_TRUE = "y";

    /** Number of seconds after which we display the full date deadline */
    private static final int FULL_DATE_THRESHOLD = 7*24*3600;

    // alarm date formatter
    private static Format alarmFormat = null;

    private final Activity activity;
    private final List<TaskModelForList> objects;
    private final int resource;
    private final LayoutInflater inflater;
    private final TaskListAdapterHooks hooks;
    private final TextView deletedItemView = new TextView(getContext());

    private final Integer fontSizePreference;
    private final AlertController alarmController;

    private TaskModelForList recentlyCompleted = null;

    /**
     * Call-back interface for interacting with parent activity
     *
     * @author timsu
     *
     */
    public interface TaskListAdapterHooks {
        List<TaskModelForList> getTaskArray();
        String getTagsFor(TaskModelForList task);
        TaskController taskController();
        TagController tagController();
        void performItemClick(View v, int position);
        void onCreatedTaskListView(View v, TaskModelForList task);

        void editItem(TaskModelForList task);
        void toggleTimerOnItem(TaskModelForList task);
        void setSelectedItem(TaskIdentifier taskId);
    }

    /**
     * Constructor
     *
     * @param activity
     * @param context
     * @param resource
     * @param objects
     * @param hooks
     */
    public TaskListAdapter(Activity activity, int resource,
            List<TaskModelForList> objects, TaskListAdapterHooks hooks) {
        super(activity, resource, objects);

        inflater = (LayoutInflater)activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        this.objects = objects;
        this.resource = resource;
        this.activity = activity;
        this.hooks = hooks;

        fontSizePreference = Preferences.getTaskListFontSize(getContext());
        alarmController = new AlertController(activity);
    }

    /** Sets the expanded state as desired */
    public void setExpanded(View view, TaskModelForList task, boolean state) {
        try {
            if(state) {
                task.clearCache();
                task.putCachedLabel(KEY_EXPANDED, CACHE_TRUE);
                hooks.setSelectedItem(task.getTaskIdentifier());
            } else {
                task.putCachedLabel(KEY_EXPANDED, null);
                hooks.setSelectedItem(null);
            }

            if(view != null) {
                setFieldContentsAndVisibility(view, task);

                // if the item is near the bottom of the list, we need to give
                // it focus so that the list knows there's new stuff down there
                int position = objects.indexOf(task);
                if(objects.size() - position < 2 && view.getParent() != null)
                    ((ListView)view.getParent()).setSelection(position);
                view.requestFocus();
            }
        } catch (Exception e) {
            AstridUtilities.reportFlurryError("task-adapter-set-expanded", e);
            Log.e("astrid", "Error in setExpanded", e);
        }
    }

    /** Toggle the expanded state of this task */
    public void toggleExpanded(View view, TaskModelForList task) {
        if(CACHE_TRUE.equals(task.getCachedLabel(KEY_EXPANDED))) {
            setExpanded(view, task, false);
        } else {
            setExpanded(view, task, true);
        }
    }

    // ----------------------------------------------------------------------
    // --- code for setting up each view
    // ----------------------------------------------------------------------

    @Override
    /** Creates or reuses the view for a row in the list */
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if(objects.size() <= position || objects.get(position) == null)
            return deletedItemView;

        if(view == null) {
            view = inflater.inflate(resource, parent, false);
            initializeView(view);
            addListeners(view);
        }

        setupView(view, objects.get(position));

        return view;
    }

    /**
     * Perform initial setup on the row. Called once for the whole list
     *
     * @param view
     */
    private void initializeView(View view) {
    	final TextView name = ((TextView)view.findViewById(R.id.task_name));
    	if(fontSizePreference != null && fontSizePreference > 0)
            name.setTextSize(fontSizePreference);
    	deletedItemView.setText(getContext().getResources().getString(R.string.taskList_deleted));
    }

    /**
     * Setup the given view for the specified task. Called every row
     *
     * @param view
     * @param task
     */
    private void setupView(View view, final TaskModelForList task) {
        Resources r = activity.getResources();

        view.setTag(task);
        setFieldContentsAndVisibility(view, task);

        if(task == null) {
            return;
        }

        final CheckBox progress = ((CheckBox)view.findViewById(R.id.cb1));
        progress.setChecked(task.isTaskCompleted());

        final ImageView timer = ((ImageView)view.findViewById(R.id.imageLeft));
        if(task.getTimerStart() != null) {
            timer.setImageDrawable(r.getDrawable(R.drawable.icon_timer));
            view.setMinimumHeight(90);
        } else {
        	timer.setImageDrawable(null);
        	view.setMinimumHeight(45);
        }

        final TextView name = ((TextView)view.findViewById(R.id.task_name));
        setTaskAppearance(task, name, progress);

        hooks.onCreatedTaskListView(view, task);
    }

    /** Helper method to set the visibility based on if there's stuff inside */
    private static void setVisibility(TextView v) {
        if(v.getText().length() > 0)
            v.setVisibility(View.VISIBLE);
        else
            v.setVisibility(View.GONE);
    }

    /** Helper method to add a line and maybe a newline */
    private static void appendLine(StringBuilder sb, String line) {
        if(line.length() == 0)
            return;

        if(sb.length() > 0)
            sb.append("\n");
        sb.append(line);
    }

    /** Helper method to set the contents and visibility of each field */
    private void setFieldContentsAndVisibility(View view, TaskModelForList task) {
        if(task == null) {
            view.setVisibility(View.GONE);
            return;
        }

        Resources r = getContext().getResources();
        TaskFieldsVisibility visibleFields = Preferences.getTaskFieldsVisibility(activity);
        boolean isExpanded = CACHE_TRUE.equals(task.getCachedLabel(KEY_EXPANDED));
        StringBuilder details = new StringBuilder();
        StringBuilder expandedDetails = new StringBuilder();

        // expanded container
        final View expandedContainer = view.findViewById(R.id.expanded_layout);
        if(expandedContainer != null)
            expandedContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // name
        final TextView name = ((TextView)view.findViewById(R.id.task_name)); {
            String cachedResult = task.getCachedLabel(KEY_NAME);
            if(cachedResult == null) {
                String nameValue = task.getName();
                if(task.getHiddenUntil() != null && task.getHiddenUntil().getTime() > System.currentTimeMillis()) {
                    nameValue = "(" + r.getString(R.string.taskList_hiddenPrefix) + ") " + nameValue;
                    task.putCachedLabel(KEY_HIDDEN, CACHE_TRUE);
                }
                cachedResult = nameValue;
                task.putCachedLabel(KEY_NAME, cachedResult);
            }
            name.setText(cachedResult);
            if(CACHE_TRUE.equals(task.getCachedLabel(KEY_HIDDEN)))
                name.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            else
                name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        // importance
        final View importance = view.findViewById(R.id.importance);
        if(visibleFields.IMPORTANCE) {
            importance.setBackgroundColor(r.getColor(
                    task.getImportance().getColorResource()));
        } else
            importance.setVisibility(View.GONE);

        // due date / completion date
        final TextView deadlines = ((TextView)view.findViewById(R.id.text_deadlines));
        if(visibleFields.DEADLINE || isExpanded) {
            String cachedResult = task.getCachedLabel(KEY_DEADLINE);
            if(cachedResult == null) {
                StringBuilder label = new StringBuilder();
                if(task.isTaskCompleted()) {
                    if(task.getCompletionDate() != null) {
                        int secondsLeft = (int)((task.getCompletionDate().getTime() -
                                System.currentTimeMillis()) / 1000);
                        String finishedTime;
                        if(Math.abs(secondsLeft) < FULL_DATE_THRESHOLD)
                            finishedTime = r.getString(R.string.ago_string,
                                    DateUtilities.getDurationString(r,
                                            Math.abs(secondsLeft), 1));
                        else
                            finishedTime = DateUtilities.getFormattedDate(activity,
                                    task.getCompletionDate());
                        label.append(r.getString(R.string.taskList_completedPrefix,
                                finishedTime));
                    }
                } else {
                    boolean taskOverdue = false;
                    if(task.getDefiniteDueDate() != null) {
                        long timeLeft = (task.getDefiniteDueDate().getTime() -
                            System.currentTimeMillis())/1000;
                        if(timeLeft > 0) {
                            if(timeLeft < FULL_DATE_THRESHOLD)
                                label.append(r.getString(R.string.taskList_dueRelativeTime)).append(" ");
                            else
                                label.append(r.getString(R.string.taskList_dueAbsoluteDate)).append(" ");
                        } else {
                            taskOverdue = true;
                            label.append(r.getString(R.string.taskList_overdueBy)).append(" ");
                            task.putCachedLabel(KEY_OVERDUE, CACHE_TRUE);
                        }

                        if(timeLeft < FULL_DATE_THRESHOLD)
                            label.append(DateUtilities.getDurationString(r,
                                (int)Math.abs(timeLeft), 1, true));
                        else
                            label.append(DateUtilities.getFormattedDate(activity,
                                    task.getDefiniteDueDate()));
                    }
                    if(!taskOverdue && task.getPreferredDueDate() != null) {
                        if(task.getDefiniteDueDate() != null)
                            label.append(" / ");
                        long timeLeft = (task.getPreferredDueDate().getTime() -
                            System.currentTimeMillis())/1000;
                        label.append(r.getString(R.string.taskList_goalPrefix)).append(" ");
                        if(timeLeft > 0) {
                            if(timeLeft < FULL_DATE_THRESHOLD)
                                label.append(r.getString(R.string.taskList_dueRelativeTime)).append(" ");
                            else
                                label.append(r.getString(R.string.taskList_dueAbsoluteDate)).append(" ");
                        } else {
                            label.append(r.getString(R.string.taskList_overdueBy)).append(" ");
                            task.putCachedLabel(KEY_OVERDUE, CACHE_TRUE);
                        }

                        if(timeLeft < FULL_DATE_THRESHOLD)
                            label.append(DateUtilities.getDurationString(r,
                                (int)Math.abs(timeLeft), 1, true));
                        else
                            label.append(DateUtilities.getFormattedDate(activity,
                                    task.getPreferredDueDate()));
                    }
                }
                cachedResult = label.toString();
                task.putCachedLabel(KEY_DEADLINE, cachedResult);
            }

            if(visibleFields.DEADLINE) {
                deadlines.setText(cachedResult);
                if(CACHE_TRUE.equals(task.getCachedLabel(KEY_OVERDUE)))
                    deadlines.setTextColor(r.getColor(R.color.taskList_dueDateOverdue));
                else
                	deadlines.setTextColor(r.getColor(R.color.taskList_details));
            } else {
                expandedDetails.append(cachedResult);
            }
        }
        setVisibility(deadlines);

        // estimated / elapsed time
        if(visibleFields.TIMES || isExpanded) {
            String cachedResult = task.getCachedLabel(KEY_TIMES);
            if(cachedResult == null) {
                Integer elapsed = task.getElapsedSeconds();
                if(task.getTimerStart() != null)
                    elapsed += (int)((System.currentTimeMillis() - task.getTimerStart().getTime())/1000L);
                Integer estimated = task.getEstimatedSeconds();
                StringBuilder label = new StringBuilder();
                if(estimated > 0) {
                    label.append(r.getString(R.string.taskList_estimatedTimePrefix)).
                        append(" ").
                        append(DateUtilities.getDurationString(r, estimated, 2));
                    if(elapsed > 0)
                        label.append(" / ");
                }
                if(elapsed > 0) {
                    label.append(r.getString(R.string.taskList_elapsedTimePrefix)).
                        append(" ").
                        append(DateUtilities.getAbbreviatedDurationString(r, elapsed, 2));
                }
                cachedResult = label.toString();
                task.putCachedLabel(KEY_TIMES, cachedResult);
            }
            if(visibleFields.TIMES)
                appendLine(details, cachedResult);
            else
                appendLine(expandedDetails, cachedResult);
        }

        // reminders
        if(visibleFields.REMINDERS || isExpanded) {
            String cachedResult = task.getCachedLabel(KEY_REMINDERS);
            if(cachedResult == null) {
                Integer notifyEvery = task.getNotificationIntervalSeconds();
                StringBuilder label = new StringBuilder();
                if(notifyEvery != null && notifyEvery > 0) {
                    label.append(r.getString(R.string.taskList_periodicReminderPrefix)).
                	    append(" ").append(DateUtilities.getDurationString(r, notifyEvery, 1, true));
                }

                try {
                    alarmController.open();
        	        List<Date> alerts = alarmController.getTaskAlerts(task.getTaskIdentifier());
        	        if(alerts.size() > 0) {
        	            Date nextAlarm = null;
        	            Date now = new Date();
        	            for(Date alert : alerts) {
        	                if(alert.after(now) && (nextAlarm == null ||
        	                        alert.before(nextAlarm)))
        	                    nextAlarm = alert;
        	            }

        	            if(nextAlarm != null) {
            	            if(label.length() > 0)
            	                label.append(". ");
            	            if(alarmFormat == null)
            	            	alarmFormat = Preferences.getDateWithTimeFormat(activity);
            	            String alarmString = alarmFormat.format(nextAlarm);
            	            label.append(r.getString(R.string.taskList_alarmPrefix) +
            	                    " " + alarmString);
        	            }
        	        }
                } finally {
                    alarmController.close();
                }
                cachedResult = label.toString();
                task.putCachedLabel(KEY_REMINDERS, cachedResult);
            }
            if(visibleFields.REMINDERS)
                appendLine(details, cachedResult);
            else
                appendLine(expandedDetails, cachedResult);
        }

        // repeats
        if(visibleFields.REPEATS || isExpanded) {
            String cachedResult = task.getCachedLabel(KEY_REPEAT);
            if(cachedResult == null) {
            	RepeatInfo repeatInfo = task.getRepeat();
            	if((task.getFlags() & TaskModelForList.FLAG_SYNC_ON_COMPLETE) > 0) {
            		cachedResult = r.getString(R.string.taskList_repeatsRemotely);
            	} else if(repeatInfo != null) {
                    cachedResult = r.getString(R.string.taskList_repeatPrefix) +
                            " " + repeatInfo.getValue() + " " +
                            r.getString(repeatInfo.getInterval().getLabelResource());
                } else {
                    cachedResult = "";
                }
                task.putCachedLabel(KEY_REPEAT, cachedResult);
            }
            if(visibleFields.REPEATS)
                appendLine(details, cachedResult);
            else
                appendLine(expandedDetails, cachedResult);
        }

        // tags
        if(visibleFields.TAGS || isExpanded) {
            String cachedResult = task.getCachedLabel(KEY_TAGS);
            if(cachedResult == null) {
                String tagString = hooks.getTagsFor(task);
                if(tagString != null && !tagString.equals(""))
                    cachedResult = r.getString(R.string.taskList_tagsPrefix) +
                        " " + tagString;
                else
                    cachedResult = "";
                task.putCachedLabel(KEY_TAGS, cachedResult);
            }
            if(visibleFields.TAGS)
                appendLine(details, cachedResult);
            else
                appendLine(expandedDetails, cachedResult);
        }

        // notes
        if(visibleFields.NOTES || isExpanded) {
            if(task.getNotes() != null && task.getNotes().length() > 0) {
        	    String notes = r.getString(R.string.taskList_notesPrefix) +
        	        " " + task.getNotes();
            	if(visibleFields.NOTES)
            	    appendLine(details, notes);
                else
                    appendLine(expandedDetails, notes);
            }
        }

        final TextView detailsView = ((TextView)view.findViewById(R.id.details));
        detailsView.setText(details.toString());
        setVisibility(detailsView);


        // expanded-only fields: creation date, ...
        if(isExpanded) {
            if(task.getCreationDate() != null) {
                String cachedResult = task.getCachedLabel(KEY_CREATION);
                if(cachedResult == null) {
                    int secondsAgo = (int) ((System.currentTimeMillis() -
                            task.getCreationDate().getTime())/1000);
                    cachedResult = r.getString(R.string.taskList_createdPrefix) + " " +
                        r.getString(R.string.ago_string, DateUtilities.getDurationString(r, Math.abs(secondsAgo), 1));
                    task.putCachedLabel(KEY_CREATION, cachedResult);
                }
                appendLine(expandedDetails, cachedResult);
            }

            final Button timerButton =
                ((Button)view.findViewById(R.id.timer));
            if(task.getTimerStart() == null)
                timerButton.setText(r.getString(R.string.startTimer_label));
            else
                timerButton.setText(r.getString(R.string.stopTimer_label));

            final TextView expandedDetailsView =
                ((TextView)view.findViewById(R.id.expanded_details));
            expandedDetailsView.setText(expandedDetails.toString());
        }

    }

    /**
     * Refresh the item given at the specified position in the list (not
     * related to task identifier number)
     *
     * @param listView the parent view to refresh
     * @param position the index of the item
     */
    public void refreshItem(ListView listView, int position) {
        TaskModelForList task = hooks.getTaskArray().get(position);
        task.clearCache();
        listView.invalidateViews();
    }

    /** Set listeners for this view. This is called once total */
    private void addListeners(View view) {
        final CheckBox progress = ((CheckBox)view.findViewById(R.id.cb1));

        // clicking the check box
        progress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                View parent = (View)v.getParent().getParent();
                TaskModelForList task = (TaskModelForList)parent.getTag();

                int newProgressPercentage;
                if(progress.isChecked())
                    newProgressPercentage =
                        TaskModelForList.getCompletedPercentage();
                else
                    newProgressPercentage = 0;

                if(newProgressPercentage != task.getProgressPercentage()) {
                    setTaskProgress(task, parent, newProgressPercentage);
                    setupView(parent, task);
                }
            }

        });

        // clicking the text field
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TaskModelForList task = (TaskModelForList)v.getTag();
                toggleExpanded(v, task);
            }
        });

        // typing while selected something
        view.setOnKeyListener(new OnKeyListener() {
        	public boolean onKey(View v, int keyCode, KeyEvent event) {
        		if(event.getAction() != KeyEvent.ACTION_UP)
        			return false;

        		// hot-key to set task priority - 1-4 or ALT + Q-R
        		Importance importance = null;
        		if(event.getNumber() >= '1' && event.getNumber() <= '4')
        		    importance = Importance.values()[event.getNumber() - '1'];

        		if(importance != null) {
        			TaskModelForList task = (TaskModelForList)v.getTag();
        			task.setImportance(importance);
        			hooks.taskController().saveTask(task, false);
        			setFieldContentsAndVisibility(v, task);
            		return true;
            	}

        		return false;
        	}
        });

        // long-clicking the text field
        view.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                TaskModelForList task = (TaskModelForList)v.getTag();
                int id = (int)task.getTaskIdentifier().getId();

                menu.add(id, CONTEXT_EDIT_ID, Menu.NONE,
                        R.string.taskList_context_edit);
                menu.add(id, CONTEXT_DELETE_ID, Menu.NONE,
                        R.string.taskList_context_delete);

                int timerTitle;
                if(task.getTimerStart() == null)
                    timerTitle = R.string.taskList_context_startTimer;
                else
                    timerTitle = R.string.taskList_context_stopTimer;
                menu.add(id, CONTEXT_TIMER_ID, Menu.NONE, timerTitle);

                if(task.getDefiniteDueDate() != null ||
                        task.getPreferredDueDate() != null)
                    menu.add(id, CONTEXT_POSTPONE_ID, Menu.NONE,
                        R.string.taskList_context_postpone);

                menu.setHeaderTitle(task.getName());
            }
        });

        // clicking one of the expanded buttons
        Button editButton = (Button)view.findViewById(R.id.edit);
        editButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                View parent = (View)v.getParent().getParent().getParent().getParent();
                TaskModelForList task = (TaskModelForList)parent.getTag();
                hooks.editItem(task);
            }
        });

        Button toggleTimerButton = (Button)view.findViewById(R.id.timer);
        toggleTimerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                View parent = (View)v.getParent().getParent().getParent().getParent();
                TaskModelForList task = (TaskModelForList)parent.getTag();
                hooks.toggleTimerOnItem(task);
            }
        });
    }

    /** Helper method to set a task's progress and then adjust its appearance
     *
     * @param task
     * @param view
     * @param progress
     */
    private void setTaskProgress(final TaskModelForList task, View view, int progress) {
        final ImageView timer = ((ImageView)view.findViewById(R.id.imageLeft));
        task.setProgressPercentage(progress);
        hooks.taskController().saveTask(task, false);

        // show this task as completed even if it has repeats
        if(progress == 100) {
            recentlyCompleted = task;
            FlurryAgent.onEvent("complete-task");
        } else {
            FlurryAgent.onEvent("uncomplete-task");
            recentlyCompleted = null;
        }

        // if our timer is on, ask if we want to stop
        if(progress == 100 && task.getTimerStart() != null) {
            new AlertDialog.Builder(activity)
            .setTitle(R.string.question_title)
            .setMessage(R.string.stop_timer_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    task.stopTimerAndUpdateElapsedTime();
                    hooks.taskController().saveTask(task, false);
                    timer.setVisibility(View.GONE);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        }
    }

    /** Helper method to adjust a tasks' apperance if the task is completed or
     * uncompleted.
     *
     * @param task
     * @param name
     * @param progress
     */
    private void setTaskAppearance(TaskModelForList task, TextView name, CheckBox progress) {
        Resources r = activity.getResources();

        if(task.isTaskCompleted() || task == recentlyCompleted) {
        	name.setPaintFlags(name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            name.setTextColor(r.getColor(R.color.task_list_done));
            progress.setButtonDrawable(R.drawable.btn_check);
            progress.setChecked(true);
        } else {
        	name.setPaintFlags(name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            name.setTextColor(r.getColor(task.getTaskColorResource(getContext())));
            progress.setButtonDrawable(R.drawable.btn_check);
        }
    }

}
