/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import java.util.Date;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.ui.DateAndTimeDialog.DateAndTimeDialogListener;

/**
 * Control set for specifying when a task should be hidden
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HideUntilControlSet extends PopupControlSet implements OnItemSelectedListener {

    private static final int SPECIFIC_DATE = -1;
    private static final int EXISTING_TIME_UNSET = -2;

    //private final CheckBox enabled;
    private Spinner spinner;
    private int previousSetting = Task.HIDE_UNTIL_NONE;
    private final int title;
    private int selection;
    private final ImageView image;

    private long existingDate = EXISTING_TIME_UNSET;

    public HideUntilControlSet(Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        this.title = title;
        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
    }

    private ArrayAdapter<HideUntilValue> adapter;

    /**
     * Container class for urgencies
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    private class HideUntilValue {
        public String label;
        public int setting;
        public long date;

        public HideUntilValue(String label, int setting) {
            this(label, setting, 0);
        }

        public HideUntilValue(String label, int setting, long date) {
            this.label = label;
            this.setting = setting;
            this.date = date;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private HideUntilValue[] createHideUntilList(long specificDate) {
        // set up base values
        String[] labels = activity.getResources().getStringArray(R.array.TEA_hideUntil);
        HideUntilValue[] values = new HideUntilValue[labels.length];
        values[0] = new HideUntilValue(labels[0], Task.HIDE_UNTIL_NONE);
        values[1] = new HideUntilValue(labels[1], Task.HIDE_UNTIL_DUE);
        values[2] = new HideUntilValue(labels[2], Task.HIDE_UNTIL_DUE_TIME);
        values[3] = new HideUntilValue(labels[3], Task.HIDE_UNTIL_DAY_BEFORE);
        values[4] = new HideUntilValue(labels[4], Task.HIDE_UNTIL_WEEK_BEFORE);
        values[5] = new HideUntilValue(labels[5], Task.HIDE_UNTIL_SPECIFIC_DAY, -1);

        if(specificDate > 0) {
            HideUntilValue[] updated = new HideUntilValue[values.length + 1];
            for(int i = 0; i < values.length; i++)
                updated[i+1] = values[i];
            Date hideUntilAsDate = new Date(specificDate);
            if(hideUntilAsDate.getHours() == 0 && hideUntilAsDate.getMinutes() == 0 && hideUntilAsDate.getSeconds() == 0) {
                updated[0] = new HideUntilValue(DateUtilities.getDateString(activity, new Date(specificDate)),
                        Task.HIDE_UNTIL_SPECIFIC_DAY, specificDate);
                existingDate = specificDate;
            } else {
                updated[0] = new HideUntilValue(DateUtilities.getDateStringWithTime(activity, new Date(specificDate)),
                        Task.HIDE_UNTIL_SPECIFIC_DAY_TIME, specificDate);
                existingDate = specificDate;
            }
            values = updated;
        }

        return values;
    }

    // --- listening for events

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // if specific date selected, show dialog
        // ... at conclusion of dialog, update our list
        HideUntilValue item = adapter.getItem(position);
        if(item.date == SPECIFIC_DATE) {
            customDate = new Date(existingDate == EXISTING_TIME_UNSET ? DateUtilities.now() : existingDate);
            customDate.setSeconds(0);

            final DateAndTimeDialog dateAndTimeDialog = new DateAndTimeDialog(activity, customDate.getTime());
            dateAndTimeDialog.show();
            dateAndTimeDialog.setDateAndTimeDialogListener(new DateAndTimeDialogListener() {
                @Override
                public void onDateAndTimeSelected(long date) {
                    if (date > 0) {
                        customDate = new Date(date);
                        if (!dateAndTimeDialog.hasTime()) {
                            customDate.setHours(0);
                            customDate.setMinutes(0);
                            customDate.setSeconds(0);
                        }
                        customDateFinished();
                    }
                }

                @Override
                public void onDateAndTimeCancelled() {
                    // user canceled, restore previous choice
                    spinner.setSelection(previousSetting);
                    refreshDisplayView();
                }
            });

            spinner.setSelection(previousSetting);
        } else {
            previousSetting = position;
        }
        selection = spinner.getSelectedItemPosition();
        refreshDisplayView();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // ignore
    }

    Date customDate;

    private void customDateFinished() {
        HideUntilValue[] list = createHideUntilList(customDate.getTime());
        adapter = new ArrayAdapter<HideUntilValue>(
                activity, android.R.layout.simple_spinner_item,
                list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        refreshDisplayView();
    }

    // --- setting up values

    public void setDefaults() {
        int setting = Preferences.getIntegerFromString(R.string.p_default_hideUntil_key,
                Task.HIDE_UNTIL_NONE);
        selection = setting;
        if (spinner != null)
            spinner.setSelection(selection);
        refreshDisplayView();
    }

    @Override
    protected OnClickListener getDisplayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spinner == null) // Force load
                    getView();
                spinner.performClick();
            }
        };
    }

    @Override
    protected void refreshDisplayView() {
        TextView auxDisplay = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        HideUntilValue value = adapter.getItem(selection);
        if (value.setting == Task.HIDE_UNTIL_NONE) {
            auxDisplay.setText(R.string.TEA_hideUntil_label);
            auxDisplay.setTextColor(unsetColor);
            image.setImageResource(R.drawable.tea_icn_hide_gray);
        } else {
            String display = value.toString();
            if (value.setting != Task.HIDE_UNTIL_SPECIFIC_DAY && value.setting != Task.HIDE_UNTIL_SPECIFIC_DAY_TIME)
                display = display.toLowerCase();

            auxDisplay.setText(activity.getString(R.string.TEA_hideUntil_display, display));
            auxDisplay.setTextColor(themeColor);
            image.setImageResource(ThemeService.getTaskEditDrawable(R.drawable.tea_icn_hide, R.drawable.tea_icn_hide_lightblue));
        }
    }

    @Override
    protected void afterInflate() {
        this.spinner = (Spinner) getView().findViewById(R.id.hideUntil);
        this.spinner.setOnItemSelectedListener(this);
        this.spinner.setPromptId(title);
        ((LinearLayout) getDisplayView()).addView(getView()); // hack to make listeners work
    }

    @Override
    public void readFromTask(Task task) {
        long date = task.getValue(Task.HIDE_UNTIL);

        Date dueDay = new Date(task.getValue(Task.DUE_DATE)/1000L*1000L);

        dueDay.setHours(0);
        dueDay.setMinutes(0);
        dueDay.setSeconds(0);

        // For the hide until due case, we need the time component
        long dueTime = task.getValue(Task.DUE_DATE)/1000L*1000L;

        if(date == 0) {
            selection = 0;
            date = 0;
        } else if(date == dueDay.getTime()) {
            selection = 1;
            date = 0;
        } else if (date == dueTime){
            selection = 2;
            date = 0;
        } else if(date + DateUtilities.ONE_DAY == dueDay.getTime()) {
            selection = 3;
            date = 0;
        } else if(date + DateUtilities.ONE_WEEK == dueDay.getTime()) {
            selection = 4;
            date = 0;
        }

        HideUntilValue[] list = createHideUntilList(date);
        adapter = new ArrayAdapter<HideUntilValue>(
                activity, android.R.layout.simple_spinner_item, list);

        super.readFromTask(task);
    }

    @Override
    protected void readFromTaskOnInitialize() {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setSelection(selection);
        refreshDisplayView();
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        if(adapter == null || spinner == null)
            return null;
        HideUntilValue item = adapter.getItem(spinner.getSelectedItemPosition());
        if(item == null)
            return null;
        long value = task.createHideUntil(item.setting, item.date);
        task.setValue(Task.HIDE_UNTIL, value);

        if (value != 0)
            return activity.getString(R.string.TEA_hideUntil_message, DateAndTimePicker.getDisplayString(activity, value, false, false, false));
        return null;
    }

}
