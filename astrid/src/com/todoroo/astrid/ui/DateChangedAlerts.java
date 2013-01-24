/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import java.text.ParseException;
import java.util.Date;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.repeats.RepeatTaskCompleteListener;
import com.todoroo.astrid.ui.DateAndTimeDialog.DateAndTimeDialogListener;
import com.todoroo.astrid.utility.Flags;

/**
 * Helper class that creates a dialog to confirm the results of a quick add markup
 * @author Sam
 *
 */
public class DateChangedAlerts {

    /** Preference key for how many of these helper dialogs we've shown */
    public static final String PREF_NUM_HELPERS_SHOWN = "pref_num_date_helpers"; //$NON-NLS-1$

    /** Preference key for whether or not we should show such dialogs */
    public static final int PREF_SHOW_HELPERS = R.string.p_showSmartConfirmation_key;

    /** Start showing the option to hide future notifs after this many confirmation dialogs */
    public static final int HIDE_CHECKBOX_AFTER_SHOWS = 3;


    public static void showQuickAddMarkupDialog(final AstridActivity activity, Task task, String originalText) {
        if (!Preferences.getBoolean(PREF_SHOW_HELPERS, true))
            return;

        final Dialog d = new Dialog(activity, R.style.ReminderDialog);
        final long taskId = task.getId();
        final boolean editable = task.isEditable();
        d.setContentView(R.layout.astrid_reminder_view);

        Button okButton = (Button) d.findViewById(R.id.reminder_complete);
        okButton.setText(R.string.DLG_ok);

        d.findViewById(R.id.reminder_snooze).setVisibility(View.GONE);
        ((TextView) d.findViewById(R.id.reminder_title)).setText(activity.getString(R.string.TLA_quickadd_confirm_title, originalText));

        Spanned speechBubbleText = constructSpeechBubbleTextForQuickAdd(activity, task);

        ((TextView) d.findViewById(R.id.reminder_message)).setText(speechBubbleText, TextView.BufferType.SPANNABLE);

        setupOkAndDismissButtons(d);
        setupHideCheckbox(d);

        d.findViewById(R.id.reminder_edit).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                activity.onTaskListItemClicked(taskId, editable);
            }
        });

        setupDialogLayoutParams(activity, d);
        d.setOwnerActivity(activity);
        d.show();
    }

    public static void showRepeatChangedDialog(final AstridActivity activity, Task task) {
        if (!Preferences.getBoolean(PREF_SHOW_HELPERS, true))
            return;

        final Dialog d = new Dialog(activity, R.style.ReminderDialog);
        d.setContentView(R.layout.astrid_reminder_view);

        Button okButton = (Button) d.findViewById(R.id.reminder_complete);
        okButton.setText(R.string.DLG_ok);

        d.findViewById(R.id.reminder_snooze).setVisibility(View.GONE);
        d.findViewById(R.id.reminder_edit).setVisibility(View.GONE);
        ((TextView) d.findViewById(R.id.reminder_title)).setText(activity.getString(R.string.TLA_repeat_scheduled_title, task.getValue(Task.TITLE)));

        String speechBubbleText = constructSpeechBubbleTextForRepeat(activity, task);

        ((TextView) d.findViewById(R.id.reminder_message)).setText(speechBubbleText);

        setupOkAndDismissButtons(d);
        setupHideCheckbox(d);

        setupDialogLayoutParams(activity, d);
        d.setOwnerActivity(activity);
        d.show();
    }


    public static final Property<?>[] REPEAT_RESCHEDULED_PROPERTIES =
            new Property<?>[] {
                    Task.ID,
                    Task.TITLE,
                    Task.DUE_DATE,
                    Task.HIDE_UNTIL,
                    Task.REPEAT_UNTIL
            };

    public static void showRepeatTaskRescheduledDialog(final AstridActivity activity, final Task task,
            final long oldDueDate, final long newDueDate, final boolean lastTime) {
        if (!Preferences.getBoolean(PREF_SHOW_HELPERS, true))
            return;

        final Dialog d = new Dialog(activity, R.style.ReminderDialog);

        d.setContentView(R.layout.astrid_reminder_view);

        Button okButton = (Button) d.findViewById(R.id.reminder_complete);
        Button undoButton = (Button) d.findViewById(R.id.reminder_edit);

        Button keepGoing = (Button) d.findViewById(R.id.reminder_snooze);
        if (!lastTime) {
            keepGoing.setVisibility(View.GONE);
        } else {
            keepGoing.setText(R.string.repeat_keep_going);
            keepGoing.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    long startDate = 0;
                    DateAndTimeDialog picker = new DateAndTimeDialog(activity, startDate, R.layout.repeat_until_dialog, R.string.repeat_until_title);
                    picker.setDateAndTimeDialogListener(new DateAndTimeDialogListener() {
                        @Override
                        public void onDateAndTimeSelected(long date) {
                            d.dismiss();
                            task.setValue(Task.REPEAT_UNTIL, date);
                            RepeatTaskCompleteListener.rescheduleTask(task, newDueDate);
                            Flags.set(Flags.REFRESH);
                        }

                        @Override
                        public void onDateAndTimeCancelled() {
                            //
                        }
                    });
                    picker.show();
                }
            });
        }

        okButton.setText(R.string.DLG_ok);
        undoButton.setText(R.string.DLG_undo);

        int titleResource = lastTime ? R.string.repeat_rescheduling_dialog_title_last_time : R.string.repeat_rescheduling_dialog_title;
        ((TextView) d.findViewById(R.id.reminder_title)).setText(
                activity.getString(titleResource, task.getValue(Task.TITLE)));

        String oldDueDateString = getRelativeDateAndTimeString(activity, oldDueDate);
        String newDueDateString = getRelativeDateAndTimeString(activity, newDueDate);
        String repeatUntilDateString = getRelativeDateAndTimeString(activity, task.getValue(Task.REPEAT_UNTIL));

        int encouragementResource = lastTime ? R.array.repeat_encouragement_last_time : R.array.repeat_encouragement;
        String[] encouragements = activity.getResources().getStringArray(encouragementResource);
        String encouragement = encouragements[(int) (Math.random()*encouragements.length)];

        String speechBubbleText;
        if (lastTime)
            speechBubbleText = activity.getString(R.string.repeat_rescheduling_dialog_bubble_last_time, repeatUntilDateString, encouragement);
        else if (!TextUtils.isEmpty(oldDueDateString))
            speechBubbleText = activity.getString(R.string.repeat_rescheduling_dialog_bubble, encouragement, oldDueDateString, newDueDateString);
        else
            speechBubbleText = activity.getString(R.string.repeat_rescheduling_dialog_bubble_no_date, encouragement, newDueDateString);

        ((TextView) d.findViewById(R.id.reminder_message)).setText(speechBubbleText);

        setupOkAndDismissButtons(d);
        setupHideCheckbox(d);

        undoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                task.setValue(Task.DUE_DATE, oldDueDate);
                task.setValue(Task.COMPLETION_DATE, 0L);
                long hideUntil = task.getValue(Task.HIDE_UNTIL);
                if (hideUntil > 0)
                    task.setValue(Task.HIDE_UNTIL, hideUntil - (newDueDate - oldDueDate));
                PluginServices.getTaskService().save(task);
                Flags.set(Flags.REFRESH);
            }
        });

        setupDialogLayoutParams(activity, d);

        d.setOwnerActivity(activity);
        d.show();
    }

    private static void setupOkAndDismissButtons(final Dialog d) {
        d.findViewById(R.id.reminder_complete).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.findViewById(R.id.dismiss).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
    }

    private static void setupHideCheckbox(final Dialog d) {
        int numShows = Preferences.getInt(PREF_NUM_HELPERS_SHOWN, 0);
        numShows++;
        if (numShows >= HIDE_CHECKBOX_AFTER_SHOWS) {
            CheckBox checkbox = (CheckBox) d.findViewById(R.id.reminders_should_show);
            checkbox.setVisibility(View.VISIBLE);
            checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Preferences.setBoolean(PREF_SHOW_HELPERS, !isChecked);
                }
            });
        }
        Preferences.setInt(PREF_NUM_HELPERS_SHOWN, numShows);
    }

    private static void setupDialogLayoutParams(Context context, Dialog d) {
        LayoutParams params = d.getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        params.height = LayoutParams.WRAP_CONTENT;
        Configuration config = context.getResources().getConfiguration();
        int size = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (AndroidUtilities.getSdkVersion() >= 9 && size == Configuration.SCREENLAYOUT_SIZE_XLARGE || size == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            params.width = metrics.widthPixels / 2;
        }
        d.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

    }


    @SuppressWarnings("nls")
    private static Spanned constructSpeechBubbleTextForQuickAdd(Context context, Task task) {
        String[] priorityStrings = context.getResources().getStringArray(R.array.TLA_priority_strings);
        int[] colorsArray = new int[] { R.color.importance_1, R.color.importance_2, R.color.importance_3, R.color.importance_4 };

        String title = task.getValue(Task.TITLE);
        long date = task.getValue(Task.DUE_DATE);

        String dueString = "";
        if (!TextUtils.isEmpty(task.getValue(Task.RECURRENCE))) {
            dueString = getRecurrenceString(context, task);
        }

        if (TextUtils.isEmpty(dueString)) {
            dueString = getRelativeDateAndTimeString(context, date);
        }

        if (!TextUtils.isEmpty(dueString))
            dueString = context.getString(R.string.TLA_quickadd_confirm_speech_bubble_date, dueString);

        int priority = task.getValue(Task.IMPORTANCE);
        if (priority >= priorityStrings.length)
            priority = priorityStrings.length - 1;
        String priorityString = priorityStrings[priority];
        int color = context.getResources().getColor(colorsArray[priority]) - 0xff000000;
        priorityString = String.format("<font color=\"#%s\">%s</font>", Integer.toHexString(color), priorityString);

        String fullString = context.getString(R.string.TLA_quickadd_confirm_speech_bubble, title, dueString, priorityString);
        return Html.fromHtml(fullString);
    }

    private static String constructSpeechBubbleTextForRepeat(Context context, Task task) {
        String recurrence = getRecurrenceString(context, task);
        return context.getString(R.string.TLA_repeat_scheduled_speech_bubble, recurrence);
    }

    @SuppressWarnings("nls")
    private static String getRelativeDateAndTimeString(Context context, long date) {
        String dueString = date > 0 ? DateUtilities.getRelativeDay(context, date, false) : "";
        if(Task.hasDueTime(date))
            dueString = String.format("%s at %s", dueString, //$NON-NLS-1$
                    DateUtilities.getTimeString(context, new Date(date)));
        return dueString;
    }

    @SuppressWarnings("nls")
    private static String getRecurrenceString(Context context, Task task) {
        try {
            RRule rrule = new RRule(task.sanitizedRecurrence());

            String[] dateAbbrev = context.getResources().getStringArray(
                    R.array.repeat_interval);
            String frequency = "";
            Frequency freq = rrule.getFreq();
            switch(freq) {
            case DAILY:
                frequency = dateAbbrev[0].toLowerCase();
                break;
            case WEEKLY:
                frequency = dateAbbrev[1].toLowerCase();
                break;
            case MONTHLY:
                frequency = dateAbbrev[2].toLowerCase();
                break;
            case HOURLY:
                frequency = dateAbbrev[3].toLowerCase();
                break;
            case MINUTELY:
                frequency = dateAbbrev[4].toLowerCase();
                break;
            case YEARLY:
                frequency = dateAbbrev[5].toLowerCase();
            }

            if (!TextUtils.isEmpty(frequency)) {
                String date = String.format("%s %s", rrule.getInterval(), frequency); //$NON-NLS-1$
                return String.format(context.getString(R.string.repeat_detail_duedate),
                        date).toLowerCase(); // Every freq int
            }

        } catch (ParseException e) {
            // Eh
        }
        return "";
    }

}
