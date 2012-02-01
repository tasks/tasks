package com.todoroo.astrid.ui;

import java.text.ParseException;
import java.util.Date;

import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.data.Task;

/**
 * Helper class that creates a dialog to confirm the results of a quick add markup
 * @author Sam
 *
 */
public class QuickAddMarkupDialog {

    public static Dialog createQuickAddMarkupDialog(final AstridActivity activity, Task task, String originalText) {
        final Dialog d = new Dialog(activity, R.style.ReminderDialog);
        final long taskId = task.getId();
        d.setContentView(R.layout.astrid_reminder_view);

        d.findViewById(R.id.reminder_snooze).setVisibility(View.GONE);
        ((Button) d.findViewById(R.id.reminder_complete)).setText(R.string.DLG_ok);
        ((TextView) d.findViewById(R.id.reminder_title)).setText(activity.getString(R.string.TLA_quickadd_confirm_title, originalText));

        Spanned speechBubbleText = constructSpeechBubbleText(activity, task);

        ((TextView) d.findViewById(R.id.reminder_message)).setText(speechBubbleText, TextView.BufferType.SPANNABLE);

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
        d.findViewById(R.id.reminder_edit).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                activity.onTaskListItemClicked(taskId);
            }
        });

        return d;
    }

    @SuppressWarnings("nls")
    private static Spanned constructSpeechBubbleText(Context context, Task task) {
        String[] priorityStrings = context.getResources().getStringArray(R.array.TLA_priority_strings);
        int[] colorsArray = new int[] { R.color.importance_1, R.color.importance_2, R.color.importance_3, R.color.importance_4 };

        String title = task.getValue(Task.TITLE);
        long date = task.getValue(Task.DUE_DATE);

        String dueDate = "";
        if (!TextUtils.isEmpty(task.getValue(Task.RECURRENCE))) {
            dueDate = getRecurrenceString(context, task);
        }

        if (TextUtils.isEmpty(dueDate)) {
            dueDate = task.getValue(Task.DUE_DATE) > 0 ? DateUtilities.getRelativeDay(context, date, false) : "";
            if(Task.hasDueTime(date))
                dueDate = String.format("%s at %s", dueDate, //$NON-NLS-1$
                        DateUtilities.getTimeString(context, new Date(date)));
        }

        if (!TextUtils.isEmpty(dueDate))
            dueDate = context.getString(R.string.TLA_quickadd_confirm_speech_bubble_date, dueDate);

        int priority = task.getValue(Task.IMPORTANCE);
        if (priority >= priorityStrings.length)
            priority = priorityStrings.length - 1;
        String priorityString = priorityStrings[priority];
        int color = context.getResources().getColor(colorsArray[priority]) - 0xff000000;
        priorityString = String.format("<font color=\"#%s\">%s</font>", Integer.toHexString(color), priorityString);

        String fullString = context.getString(R.string.TLA_quickadd_confirm_speech_bubble, title, dueDate, priorityString);
        return Html.fromHtml(fullString);
    }

    private static String getRecurrenceString(Context context, Task task) {
        try {
            RRule rrule = new RRule(task.getValue(Task.RECURRENCE));

            String[] dateAbbrev = context.getResources().getStringArray(
                    R.array.repeat_interval);
            String frequency = "";
            Frequency freq = rrule.getFreq();
            if (freq == Frequency.DAILY) {
                frequency = dateAbbrev[0].toLowerCase();
            } else if (freq == Frequency.WEEKLY) {
                frequency = dateAbbrev[1].toLowerCase();
            } else if (freq == Frequency.MONTHLY) {
                frequency = dateAbbrev[2].toLowerCase();
            } else if (freq == Frequency.HOURLY) {
                frequency = dateAbbrev[3].toLowerCase();
            } else if (freq == Frequency.MINUTELY) {
                frequency = dateAbbrev[4].toLowerCase();
            } else if (freq == Frequency.YEARLY) {
                frequency = dateAbbrev[5].toLowerCase();
            }
            if (!TextUtils.isEmpty(frequency)) {
                String date = String.format("%s %s", rrule.getInterval(), frequency); //$NON-NLS-1$
                return String.format(context.getString(R.string.repeat_detail_duedate), date).toLowerCase(); // Every freq int
            }

        } catch (ParseException e) {
            // Eh
        }
        return "";
    }

}
