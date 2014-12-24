/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
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
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.repeats.RepeatTaskCompleteListener;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.ui.DateAndTimeDialog.DateAndTimeDialogListener;
import com.todoroo.astrid.utility.Flags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.ActivityPreferences;

import java.text.ParseException;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastGingerbread;
import static org.tasks.date.DateTimeUtils.newDate;

/**
 * Helper class that creates a dialog to confirm the results of a quick add markup
 * @author Sam
 *
 */
public class DateChangedAlerts {

    private static final Logger log = LoggerFactory.getLogger(DateChangedAlerts.class);

    /** Preference key for how many of these helper dialogs we've shown */
    private static final String PREF_NUM_HELPERS_SHOWN = "pref_num_date_helpers"; //$NON-NLS-1$

    /** Preference key for whether or not we should show such dialogs */
    private static final int PREF_SHOW_HELPERS = R.string.p_showSmartConfirmation_key;

    /** Start showing the option to hide future notifs after this many confirmation dialogs */
    private static final int HIDE_CHECKBOX_AFTER_SHOWS = 3;

    private final Context context;
    private final ActivityPreferences preferences;

    @Inject
    public DateChangedAlerts(@ForApplication Context context, ActivityPreferences preferences) {
        this.context = context;
        this.preferences = preferences;
    }

    public void showQuickAddMarkupDialog(final AstridActivity activity, Task task, String originalText) {
        if (!preferences.getBoolean(PREF_SHOW_HELPERS, true)) {
            return;
        }

        final Dialog d = new Dialog(activity, R.style.ReminderDialog);
        final long taskId = task.getId();
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
                activity.onTaskListItemClicked(taskId);
            }
        });

        setupDialogLayoutParams(activity, d);
        d.setOwnerActivity(activity);
        d.show();
    }

    public void showRepeatChangedDialog(final Activity activity, Task task) {
        if (!preferences.getBoolean(PREF_SHOW_HELPERS, true)) {
            return;
        }

        final Dialog d = new Dialog(activity, R.style.ReminderDialog);
        d.setContentView(R.layout.astrid_reminder_view);

        Button okButton = (Button) d.findViewById(R.id.reminder_complete);
        okButton.setText(R.string.DLG_ok);

        d.findViewById(R.id.reminder_snooze).setVisibility(View.GONE);
        d.findViewById(R.id.reminder_edit).setVisibility(View.GONE);
        ((TextView) d.findViewById(R.id.reminder_title)).setText(activity.getString(R.string.TLA_repeat_scheduled_title, task.getTitle()));

        String speechBubbleText = constructSpeechBubbleTextForRepeat(activity, task);

        ((TextView) d.findViewById(R.id.reminder_message)).setText(speechBubbleText);

        setupOkAndDismissButtons(d);
        setupHideCheckbox(d);

        setupDialogLayoutParams(activity, d);
        d.setOwnerActivity(activity);
        d.show();
    }

    public void showRepeatTaskRescheduledDialog(final GCalHelper gcalHelper, final TaskService taskService, final Activity activity, final Task task,
            final long oldDueDate, final long newDueDate, final boolean lastTime) {
        if (!preferences.getBoolean(PREF_SHOW_HELPERS, true)) {
            return;
        }

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
                    DateAndTimeDialog picker = new DateAndTimeDialog(preferences, activity, startDate, R.layout.repeat_until_dialog, R.string.repeat_until_title);
                    picker.setDateAndTimeDialogListener(new DateAndTimeDialogListener() {
                        @Override
                        public void onDateAndTimeSelected(long date) {
                            d.dismiss();
                            task.setRepeatUntil(date);
                            RepeatTaskCompleteListener.rescheduleTask(context, gcalHelper, taskService, task, newDueDate);
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
                activity.getString(titleResource, task.getTitle()));

        String oldDueDateString = getRelativeDateAndTimeString(activity, oldDueDate);
        String newDueDateString = getRelativeDateAndTimeString(activity, newDueDate);
        String repeatUntilDateString = getRelativeDateAndTimeString(activity, task.getRepeatUntil());

        String encouragement = "";

        String speechBubbleText;
        if (lastTime) {
            speechBubbleText = activity.getString(R.string.repeat_rescheduling_dialog_bubble_last_time, repeatUntilDateString, encouragement);
        } else if (!TextUtils.isEmpty(oldDueDateString)) {
            speechBubbleText = activity.getString(R.string.repeat_rescheduling_dialog_bubble, encouragement, oldDueDateString, newDueDateString);
        } else {
            speechBubbleText = activity.getString(R.string.repeat_rescheduling_dialog_bubble_no_date, encouragement, newDueDateString);
        }

        ((TextView) d.findViewById(R.id.reminder_message)).setText(speechBubbleText);

        setupOkAndDismissButtons(d);
        setupHideCheckbox(d);

        undoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                task.setDueDate(oldDueDate);
                task.setCompletionDate(0L);
                long hideUntil = task.getHideUntil();
                if (hideUntil > 0) {
                    task.setHideUntil(hideUntil - (newDueDate - oldDueDate));
                }
                taskService.save(task);
                Flags.set(Flags.REFRESH);
            }
        });

        setupDialogLayoutParams(activity, d);

        d.setOwnerActivity(activity);
        d.show();
    }

    private void setupOkAndDismissButtons(final Dialog d) {
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

    private void setupHideCheckbox(final Dialog d) {
        int numShows = preferences.getInt(PREF_NUM_HELPERS_SHOWN, 0);
        numShows++;
        if (numShows >= HIDE_CHECKBOX_AFTER_SHOWS) {
            CheckBox checkbox = (CheckBox) d.findViewById(R.id.reminders_should_show);
            checkbox.setVisibility(View.VISIBLE);
            checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    preferences.setBoolean(PREF_SHOW_HELPERS, !isChecked);
                }
            });
        }
        preferences.setInt(PREF_NUM_HELPERS_SHOWN, numShows);
    }

    private void setupDialogLayoutParams(Context context, Dialog d) {
        LayoutParams params = d.getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        params.height = LayoutParams.WRAP_CONTENT;
        Configuration config = context.getResources().getConfiguration();
        int size = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (atLeastGingerbread() && size == Configuration.SCREENLAYOUT_SIZE_XLARGE || size == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            params.width = metrics.widthPixels / 2;
        }
        d.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

    }

    private Spanned constructSpeechBubbleTextForQuickAdd(Context context, Task task) {
        String[] priorityStrings = context.getResources().getStringArray(R.array.TLA_priority_strings);
        int[] colorsArray = new int[] { R.color.importance_1, R.color.importance_2, R.color.importance_3, R.color.importance_4 };

        String title = task.getTitle();
        long date = task.getDueDate();

        String dueString = "";
        if (!TextUtils.isEmpty(task.getRecurrence())) {
            dueString = getRecurrenceString(context, task);
        }

        if (TextUtils.isEmpty(dueString)) {
            dueString = getRelativeDateAndTimeString(context, date);
        }

        if (!TextUtils.isEmpty(dueString)) {
            dueString = context.getString(R.string.TLA_quickadd_confirm_speech_bubble_date, dueString);
        }

        int priority = task.getImportance();
        if (priority >= priorityStrings.length) {
            priority = priorityStrings.length - 1;
        }
        String priorityString = priorityStrings[priority];
        int color = context.getResources().getColor(colorsArray[priority]) - 0xff000000;
        priorityString = String.format("<font color=\"#%s\">%s</font>", Integer.toHexString(color), priorityString);

        String fullString = context.getString(R.string.TLA_quickadd_confirm_speech_bubble, title, dueString, priorityString);
        return Html.fromHtml(fullString);
    }

    private String constructSpeechBubbleTextForRepeat(Context context, Task task) {
        String recurrence = getRecurrenceString(context, task);
        return context.getString(R.string.TLA_repeat_scheduled_speech_bubble, recurrence);
    }

    private String getRelativeDateAndTimeString(Context context, long date) {
        String dueString = date > 0 ? DateUtilities.getRelativeDay(context, date, false) : "";
        if(Task.hasDueTime(date)) {
            dueString = String.format("%s at %s", dueString, //$NON-NLS-1$
                    DateUtilities.getTimeString(context, newDate(date)));
        }
        return dueString;
    }

    private String getRecurrenceString(Context context, Task task) {
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
            log.error(e.getMessage(), e);
        }
        return "";
    }
}
