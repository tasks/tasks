package com.todoroo.astrid.ui;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.repeats.RepeatControlSet;

public class DeadlineControlSet extends PopupControlSet {

    private boolean isQuickadd = false;
    private DateAndTimePicker dateAndTimePicker;
    private final View[] extraViews;
    private final RepeatControlSet repeatControlSet;

    public DeadlineControlSet(Activity activity, int viewLayout, int displayViewLayout,
            RepeatControlSet repeatControlSet, View...extraViews) {
        super(activity, viewLayout, displayViewLayout, 0);
        this.extraViews = extraViews;
        this.displayText.setText(activity.getString(R.string.TEA_when_header_label));
        this.repeatControlSet = repeatControlSet;
    }

    @Override
    protected void refreshDisplayView() {
        StringBuilder displayString = new StringBuilder();
        if (initialized)
            displayString.append(dateAndTimePicker.getDisplayString(activity, isQuickadd, isQuickadd));
        else
            displayString.append(DateAndTimePicker.getDisplayString(activity, model.getValue(Task.DUE_DATE), isQuickadd, isQuickadd));

        if (!isQuickadd && repeatControlSet != null) {
            String repeatString = repeatControlSet.getStringForExternalDisplay();
            if (!TextUtils.isEmpty(repeatString)) {
                displayString.append("\n"); //$NON-NLS-1$
                displayString.append(repeatString);
            }
        }
        TextView dateDisplay = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        dateDisplay.setText(displayString);
    }

    @Override
    protected void afterInflate() {
        dateAndTimePicker = (DateAndTimePicker) getView().findViewById(R.id.date_and_time);
        LinearLayout extras = (LinearLayout) getView().findViewById(R.id.datetime_extras);
        for (View v : extraViews) {
            LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.0f);
            extras.addView(v, lp);
        }

        Button okButton = (Button) LayoutInflater.from(activity).inflate(R.layout.control_dialog_ok, null);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onOkClick();
                DialogUtilities.dismissDialog(DeadlineControlSet.this.activity, DeadlineControlSet.this.dialog);
            }
        });
        LinearLayout body = (LinearLayout) getView().findViewById(R.id.datetime_body);
        body.addView(okButton);
    }

    @Override
    protected void readFromTaskPrivate() {
        long dueDate = model.getValue(Task.DUE_DATE);
        initializeWithDate(dueDate);
        refreshDisplayView();
    }

    @Override
    protected String writeToModelPrivate(Task task) {
        long dueDate = dateAndTimePicker.constructDueDate();
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
