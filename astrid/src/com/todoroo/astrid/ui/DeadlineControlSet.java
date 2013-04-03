/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.ThemeService;

public class DeadlineControlSet extends PopupControlSet {

    private boolean isQuickadd = false;
    private DateAndTimePicker dateAndTimePicker;
    private final View[] extraViews;
    private final RepeatControlSet repeatControlSet;
    private final ImageView image;

    public DeadlineControlSet(Activity activity, int viewLayout, int displayViewLayout,
            RepeatControlSet repeatControlSet, View...extraViews) {
        super(activity, viewLayout, displayViewLayout, 0);
        this.extraViews = extraViews;
        this.repeatControlSet = repeatControlSet;
        this.image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
    }

    @Override
    protected void refreshDisplayView() {
        StringBuilder displayString = new StringBuilder();
        boolean isOverdue;
        if (initialized) {
            isOverdue = !dateAndTimePicker.isAfterNow();
            displayString.append(dateAndTimePicker.getDisplayString(activity, isQuickadd, isQuickadd));
        } else {
            isOverdue = model.getValue(Task.DUE_DATE) < DateUtilities.now();
            displayString.append(DateAndTimePicker.getDisplayString(activity, model.getValue(Task.DUE_DATE), isQuickadd, isQuickadd, false));
        }

        if (!isQuickadd && repeatControlSet != null) {
            String repeatString = repeatControlSet.getStringForExternalDisplay();
            if (!TextUtils.isEmpty(repeatString)) {
                displayString.append("\n"); //$NON-NLS-1$
                displayString.append(repeatString);
            }
        }
        TextView dateDisplay = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        if (TextUtils.isEmpty(displayString)) {
            dateDisplay.setText(R.string.TEA_deadline_hint);
            dateDisplay.setTextColor(unsetColor);
            image.setImageResource(R.drawable.tea_icn_date_gray);
        } else {
            dateDisplay.setText(displayString);
            if (isOverdue) {
                dateDisplay.setTextColor(activity.getResources().getColor(R.color.red_theme_color));
                image.setImageResource(R.drawable.tea_icn_date_red);
            } else {
                dateDisplay.setTextColor(themeColor);
                image.setImageResource(ThemeService.getTaskEditDrawable(R.drawable.tea_icn_date, R.drawable.tea_icn_date_lightblue));
            }
        }

    }

    @Override
    protected void afterInflate() {
        dateAndTimePicker = (DateAndTimePicker) getView().findViewById(R.id.date_and_time);
        LinearLayout extras = (LinearLayout) getView().findViewById(R.id.datetime_extras);
        for (View v : extraViews) {
            LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.0f);
            extras.addView(v, lp);
        }

        LinearLayout body = (LinearLayout) getView().findViewById(R.id.datetime_body);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        Button okButton = (Button) LayoutInflater.from(activity).inflate(R.layout.control_dialog_ok, null);
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, (int) (45 * metrics.density));
        body.addView(okButton, params);
    }

    @Override
    protected void setupOkButton(View view) {
        super.setupOkButton(view);
        Button okButton = (Button) view.findViewById(R.id.edit_dlg_ok);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onOkClick();
                DialogUtilities.dismissDialog(DeadlineControlSet.this.activity, DeadlineControlSet.this.dialog);
            }
        });
    }

    @Override
    protected void readFromTaskOnInitialize() {
        long dueDate = model.getValue(Task.DUE_DATE);
        initializeWithDate(dueDate);
        refreshDisplayView();
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        long dueDate = dateAndTimePicker.constructDueDate();
        if (dueDate != task.getValue(Task.DUE_DATE)) // Clear snooze if due date has changed
            task.setValue(Task.REMINDER_SNOOZE, 0L);
        task.setValue(Task.DUE_DATE, dueDate);
        return null;
    }

    private void initializeWithDate(long dueDate) {
        dateAndTimePicker.initializeWithDate(dueDate);
    }

    public boolean isDeadlineSet() {
        return (dateAndTimePicker != null && dateAndTimePicker.constructDueDate() != 0);
    }

    /**
     * Set whether date and time should be separated by a newline or a comma
     * in the display view
     */
    public void setIsQuickadd(boolean isQuickadd) {
        this.isQuickadd = isQuickadd;
    }
}
