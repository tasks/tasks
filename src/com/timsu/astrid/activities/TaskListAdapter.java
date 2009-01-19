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

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.timsu.astrid.R;
import com.timsu.astrid.data.alerts.AlertController;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskModelForList;
import com.timsu.astrid.data.task.AbstractTaskModel.RepeatInfo;
import com.timsu.astrid.utilities.DateUtilities;
import com.timsu.astrid.utilities.Preferences;
import com.timsu.astrid.utilities.TaskFieldsVisibility;

/** Adapter for displaying a list of TaskModelForList entities
 *
 * @author timsu
 *
 */
public class TaskListAdapter extends ArrayAdapter<TaskModelForList> {

    public static final int CONTEXT_EDIT_ID       = Menu.FIRST + 50;
    public static final int CONTEXT_DELETE_ID     = Menu.FIRST + 51;
    public static final int CONTEXT_TIMER_ID      = Menu.FIRST + 52;

    private final Activity activity;
    private List<TaskModelForList> objects;
    private int resource;
    private LayoutInflater inflater;
    private TaskListAdapterHooks hooks;

    private Integer fontSizePreference;
    private AlertController alarmController;

    public interface TaskListAdapterHooks {
        List<TaskModelForList> getTaskArray();
        List<TagModelForView> getTagsFor(TaskModelForList task);
        TaskController getTaskController();
        TagController getTagController();
        void performItemClick(View v, int position);
        void onCreatedTaskListView(View v, TaskModelForList task);
    }

    public TaskListAdapter(Activity activity, Context context, int resource,
            List<TaskModelForList> objects, TaskListAdapterHooks hooks) {
        super(context, resource, objects);

        inflater = (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        this.objects = objects;
        this.resource = resource;
        this.activity = activity;
        this.hooks = hooks;

        fontSizePreference = Preferences.getTaskListFontSize(getContext());
        alarmController = new AlertController(activity);
    }

    // --- code for setting up each view

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        view = inflater.inflate(resource, parent, false);
        setupView(view, objects.get(position));
        addListeners(position, view);

        return view;
    }

    private void setupView(View view, final TaskModelForList task) {
        Resources r = activity.getResources();

        final CheckBox progress = ((CheckBox)view.findViewById(R.id.cb1));
        final ImageView timer = ((ImageView)view.findViewById(R.id.imageLeft));
        final TextView name = ((TextView)view.findViewById(R.id.task_name));

        view.setTag(task);
        progress.setTag(task);

        if(task.getTimerStart() != null)
            timer.setImageDrawable(r.getDrawable(R.drawable.ic_dialog_time));
        progress.setChecked(task.isTaskCompleted());

        setFieldContentsAndVisibility(view, task);
        setTaskAppearance(task, name, progress);
        hooks.onCreatedTaskListView(view, task);
    }

    /** Helper method to set the visibility based on if there's stuff inside */
    private void setVisibility(TextView v) {
        if(v.getText().length() > 0)
            v.setVisibility(View.VISIBLE);
        else
            v.setVisibility(View.GONE);
    }

    /** Helper method to set the contents and visibility of each field */
    private void setFieldContentsAndVisibility(View view, TaskModelForList task) {
        TaskFieldsVisibility visibleFields = Preferences.getTaskFieldsVisibility(activity);
        Resources r = getContext().getResources();

        // name
        final TextView name = ((TextView)view.findViewById(R.id.task_name));
        if(visibleFields.TITLE) {
            String nameValue = task.getName();
            if(task.getHiddenUntil() != null && task.getHiddenUntil().after(new Date())) {
                nameValue = "(" + r.getString(R.string.taskList_hiddenPrefix) + ") " + nameValue;
                name.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            }
            name.setText(nameValue);
            if(fontSizePreference != null && fontSizePreference > 0)
                name.setTextSize(fontSizePreference);
        }
        setVisibility(name);


        // importance
        if(visibleFields.IMPORTANCE) {
            switch(task.getImportance()) {
            case LEVEL_1:
                view.findViewById(R.id.importance_1).setVisibility(View.VISIBLE);
                break;
            case LEVEL_2:
                view.findViewById(R.id.importance_2).setVisibility(View.VISIBLE);
                break;
            case LEVEL_3:
                view.findViewById(R.id.importance_3).setVisibility(View.VISIBLE);                break;
            case LEVEL_4:
                view.findViewById(R.id.importance_4).setVisibility(View.VISIBLE);
            }
        }

        // due date / completion date
        final TextView deadlines = ((TextView)view.findViewById(R.id.text_deadlines));
        if(visibleFields.DEADLINE) {
            StringBuilder label = new StringBuilder();
            if(task.isTaskCompleted()) {
                if(task.getCompletionDate() != null) {
                    int secondsLeft = (int)((task.getCompletionDate().getTime() -
                            System.currentTimeMillis()) / 1000);
                    label.append(r.getString(R.string.taskList_completedPrefix)).
                        append(" ").
                        append(DateUtilities.getDurationString(r, Math.abs(secondsLeft), 1)).
                        append(" " + r.getString(R.string.ago_suffix));
                }
            } else {
                boolean taskOverdue = false;
                if(task.getDefiniteDueDate() != null) {
                    long timeLeft = task.getDefiniteDueDate().getTime() -
                        System.currentTimeMillis();
                    if(timeLeft > 0){
                        label.append(r.getString(R.string.taskList_dueIn)).append(" ");
                    } else {
                        taskOverdue = true;
                        label.append(r.getString(R.string.taskList_overdueBy)).append(" ");
                        deadlines.setTextColor(r.getColor(R.color.taskList_dueDateOverdue));
                    }
                    label.append(DateUtilities.getDurationString(r,
                            (int)Math.abs(timeLeft/1000), 1));
                }
                if(!taskOverdue && task.getPreferredDueDate() != null) {
                    if(task.getDefiniteDueDate() != null)
                        label.append(" / ");
                    long timeLeft = task.getPreferredDueDate().getTime() -
                        System.currentTimeMillis();
                    label.append(r.getString(R.string.taskList_goalPrefix)).append(" ");
                    if(timeLeft > 0){
                        label.append(r.getString(R.string.taskList_dueIn)).append(" ");
                    } else {
                        label.append(r.getString(R.string.taskList_overdueBy)).append(" ");
                        deadlines.setTextColor(r.getColor(R.color.taskList_dueDateOverdue));
                    }
                    label.append(DateUtilities.getDurationString(r,
                            (int)Math.abs(timeLeft/1000), 1)).append(" ");
                }
            }
            deadlines.setText(label);
        }
        setVisibility(deadlines);

        // estimated / elapsed time
        final TextView times = ((TextView)view.findViewById(R.id.text_times));
        if(visibleFields.TIMES) {
            Integer elapsed = task.getElapsedSeconds();
            if(task.getTimerStart() != null)
                elapsed += ((System.currentTimeMillis() - task.getTimerStart().getTime())/1000);
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
                    append(DateUtilities.getDurationString(r, elapsed, 2));
            }
            times.setText(label);
        }
        setVisibility(times);

        // reminders
        final TextView reminders = ((TextView)view.findViewById(R.id.text_reminders));
        if(visibleFields.REMINDERS) {
            Integer notifyEvery = task.getNotificationIntervalSeconds();
            StringBuilder label = new StringBuilder();
            if(notifyEvery != null && notifyEvery > 0) {
                label.append(r.getString(R.string.taskList_periodicReminderPrefix)).
            	    append(" ").append(DateUtilities.getDurationString(r, notifyEvery, 1));
            }

            try {
                alarmController.open();
    	        List<Date> alerts = alarmController.getTaskAlerts(task.getTaskIdentifier());
    	        if(alerts.size() > 0) {
    	            if(label.length() > 0)
    	                label.append(". ");
    	            label.append(r.getQuantityString(R.plurals.Nalarms, alerts.size(),
    	                    alerts.size())).append(" ").append(r.getString(R.string.taskList_alarmSuffix));
    	        }
            } finally {
                alarmController.close();
            }
            reminders.setText(label);
        }
        setVisibility(reminders);

        // repeats
        final TextView repeats = ((TextView)view.findViewById(R.id.text_repeats));
        if(visibleFields.REPEATS) {
            RepeatInfo repeatInfo = task.getRepeat();
            if(repeatInfo != null) {
                repeats.setText(r.getString(R.string.taskList_repeatPrefix) +
                        " " + repeatInfo.getValue() + " " +
                        r.getString(repeatInfo.getInterval().getLabelResource()));
            }
        }
        setVisibility(repeats);

        // tags
        final TextView tags = ((TextView)view.findViewById(R.id.text_tags));
        if(visibleFields.TAGS) {
            List<TagModelForView> alltags = hooks.getTagsFor(task);
            StringBuilder tagString = new StringBuilder();
            for(Iterator<TagModelForView> i = alltags.iterator(); i.hasNext(); ) {
                TagModelForView tag = i.next();
                tagString.append(tag.getName());
                if(i.hasNext())
                    tagString.append(", ");
            }
            if(alltags.size() > 0)
                tags.setText(r.getString(R.string.taskList_tagsPrefix) + " " + tagString);
        }
        setVisibility(tags);

        // notes
        final TextView notes = ((TextView)view.findViewById(R.id.text_notes));
        if(visibleFields.NOTES) {
        	notes.setText(r.getString(R.string.taskList_notesPrefix) + " " + task.getNotes());
        }
        setVisibility(notes);
    }

    private void addListeners(final int position, final View view) {
        final CheckBox progress = ((CheckBox)view.findViewById(R.id.cb1));

        // clicking the check box
        progress.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                TaskModelForList task = (TaskModelForList)buttonView.getTag();

                int newProgressPercentage;
                if(isChecked)
                    newProgressPercentage =
                        TaskModelForList.getCompletedPercentage();
                else
                    newProgressPercentage = 0;

                if(newProgressPercentage != task.getProgressPercentage()) {
                    setTaskProgress(task, view, newProgressPercentage);
                    setupView(view, task);
                }
            }
        });

        // clicking the text field
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hooks.performItemClick(view, position);
            }
        });

        // long-clicking the text field
        view.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                TaskModelForList task = (TaskModelForList)v.getTag();

                menu.add(position, CONTEXT_EDIT_ID, Menu.NONE,
                        R.string.taskList_context_edit);
                menu.add(position, CONTEXT_DELETE_ID, Menu.NONE,
                        R.string.taskList_context_delete);

                int timerTitle;
                if(task.getTimerStart() == null)
                    timerTitle = R.string.taskList_context_startTimer;
                else
                    timerTitle = R.string.taskList_context_stopTimer;
                menu.add(position, CONTEXT_TIMER_ID, Menu.NONE, timerTitle);

                menu.setHeaderTitle(task.getName());
            }
        });
    }

    private void setTaskProgress(final TaskModelForList task, View view, int progress) {
        final ImageView timer = ((ImageView)view.findViewById(R.id.imageLeft));
        task.setProgressPercentage(progress);
        hooks.getTaskController().saveTask(task);

        // if our timer is on, ask if we want to stop
        if(progress == 100 && task.getTimerStart() != null) {
            new AlertDialog.Builder(activity)
            .setTitle(R.string.question_title)
            .setMessage(R.string.stop_timer_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    task.stopTimerAndUpdateElapsedTime();
                    hooks.getTaskController().saveTask(task);
                    timer.setVisibility(View.GONE);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        }
    }

    private void setTaskAppearance(TaskModelForList task, TextView name, CheckBox progress) {
        Resources r = activity.getResources();

        if(task.isTaskCompleted()) {
            name.setBackgroundDrawable(r.getDrawable(R.drawable.strikeout));
            name.setTextColor(r.getColor(R.color.task_list_done));
            progress.setButtonDrawable(R.drawable.btn_check0);
        } else {
            name.setBackgroundDrawable(null);
            name.setTextColor(r.getColor(task.getTaskColorResource(getContext())));

            if(task.getProgressPercentage() >= 75)
                progress.setButtonDrawable(R.drawable.btn_check75);
            else if(task.getProgressPercentage() >= 50)
                progress.setButtonDrawable(R.drawable.btn_check50);
            else if(task.getProgressPercentage() >= 25)
                progress.setButtonDrawable(R.drawable.btn_check25);
            else
                progress.setButtonDrawable(R.drawable.btn_check0);
        }
    }

}