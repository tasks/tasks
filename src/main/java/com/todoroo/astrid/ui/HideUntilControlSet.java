/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.tasks.R;
import org.tasks.activities.DateAndTimePickerActivity;
import org.tasks.time.DateTime;
import org.tasks.ui.HiddenTopArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.tasks.date.DateTimeUtils.newDateTime;

/**
 * Control set for specifying when a task should be hidden
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HideUntilControlSet extends TaskEditControlSetBase implements OnItemSelectedListener {

    private static final int SPECIFIC_DATE = -1;
    private static final int EXISTING_TIME_UNSET = -2;
    public static final int REQUEST_HIDE_UNTIL = 11011;

    //private final CheckBox enabled;
    private Spinner spinner;
    private int previousSetting = Task.HIDE_UNTIL_NONE;
    private int selection;

    private long existingDate = EXISTING_TIME_UNSET;
    private TaskEditFragment taskEditFragment;
    private TextView textDisplay;
    private ImageView clearButton;
    private final List<HideUntilValue> spinnerItems = new ArrayList<>();

    public HideUntilControlSet(TaskEditFragment taskEditFragment) {
        super(taskEditFragment.getActivity(), R.layout.control_set_hide);
        this.taskEditFragment = taskEditFragment;
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

    private void updateSpinnerOptions(long specificDate) {
        spinnerItems.clear();
        // set up base values
        String[] labels = activity.getResources().getStringArray(R.array.TEA_hideUntil);
        spinnerItems.addAll(new ArrayList<>(asList(
                new HideUntilValue(labels[0], Task.HIDE_UNTIL_DUE),
                new HideUntilValue(labels[1], Task.HIDE_UNTIL_DUE_TIME),
                new HideUntilValue(labels[2], Task.HIDE_UNTIL_DAY_BEFORE),
                new HideUntilValue(labels[3], Task.HIDE_UNTIL_WEEK_BEFORE),
                new HideUntilValue(labels[4], Task.HIDE_UNTIL_SPECIFIC_DAY, -1))));

        if(specificDate > 0) {
            DateTime hideUntilAsDate = newDateTime(specificDate);
            if(hideUntilAsDate.getHourOfDay() == 0 && hideUntilAsDate.getMinuteOfHour() == 0 && hideUntilAsDate.getSecondOfMinute() == 0) {
                spinnerItems.add(0, new HideUntilValue(DateUtilities.getDateString(newDateTime(specificDate)),
                        Task.HIDE_UNTIL_SPECIFIC_DAY, specificDate));
            } else {
                spinnerItems.add(0, new HideUntilValue(DateUtilities.getDateStringWithTime(activity, newDateTime(specificDate)),
                        Task.HIDE_UNTIL_SPECIFIC_DAY_TIME, specificDate));
            }
            existingDate = specificDate;
        } else {
            spinnerItems.add(0, new HideUntilValue("", Task.HIDE_UNTIL_NONE));
            existingDate = EXISTING_TIME_UNSET;
        }
        adapter.notifyDataSetChanged();
    }

    // --- listening for events

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // if specific date selected, show dialog
        // ... at conclusion of dialog, update our list
        HideUntilValue item = adapter.getItem(position);
        if(item.date == SPECIFIC_DATE) {
            customDate =
                    newDateTime(existingDate == EXISTING_TIME_UNSET ? DateUtilities.now() : existingDate)
                            .withSecondOfMinute(0);

            taskEditFragment.startActivityForResult(new Intent(taskEditFragment.getActivity(), DateAndTimePickerActivity.class) {{
                putExtra(DateAndTimePickerActivity.EXTRA_TIMESTAMP, customDate.getMillis());
            }}, REQUEST_HIDE_UNTIL);
            spinner.setSelection(previousSetting);
        } else {
            previousSetting = position;
        }
        selection = spinner.getSelectedItemPosition();
        refreshDisplayView();
    }

    public void setCustomDate(long timestamp) {
        customDate = new DateTime(timestamp);
        customDateFinished();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // ignore
    }

    DateTime customDate;

    private void customDateFinished() {
        updateSpinnerOptions(customDate.getMillis());
        spinner.setSelection(0);
        refreshDisplayView();
    }

    // --- setting up values

    private void refreshDisplayView() {
        HideUntilValue value = adapter.getItem(selection);
        if (value.setting == Task.HIDE_UNTIL_NONE) {
            textDisplay.setText(R.string.TEA_hideUntil_label);
            textDisplay.setTextColor(unsetColor);
            clearButton.setVisibility(View.GONE);
        } else {
            String display = value.toString();
            if (value.setting != Task.HIDE_UNTIL_SPECIFIC_DAY && value.setting != Task.HIDE_UNTIL_SPECIFIC_DAY_TIME) {
                display = display.toLowerCase();
            }

            textDisplay.setText(activity.getString(R.string.TEA_hideUntil_display, display));
            textDisplay.setTextColor(themeColor);
            clearButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void afterInflate() {
        textDisplay = (TextView) getView().findViewById(R.id.display_row_edit);
        clearButton = (ImageView) getView().findViewById(R.id.clear_hide_date);
        clearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSpinnerOptions(0);
                selection = 0;
                spinner.setSelection(selection);
                refreshDisplayView();
            }
        });
        textDisplay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spinner == null) {
                    getView();
                }
                spinner.performClick();
            }
        });
        this.spinner = (Spinner) getView().findViewById(R.id.hideUntil);
        adapter = new HiddenTopArrayAdapter<>(activity, android.R.layout.simple_spinner_item, spinnerItems);
        spinner.setAdapter(adapter);
        this.spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void readFromTask(Task task) {
        long date = task.getHideUntil();

        DateTime dueDay = newDateTime(task.getDueDate())
                .withHourOfDay(0)
                .withMinuteOfHour(0)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);

        // For the hide until due case, we need the time component
        long dueTime = task.getDueDate()/1000L*1000L;

        if(date == 0) {
            selection = 0;
            date = 0;
        } else if(date == dueDay.getMillis()) {
            selection = 1;
            date = 0;
        } else if (date == dueTime){
            selection = 2;
            date = 0;
        } else if(date + DateUtilities.ONE_DAY == dueDay.getMillis()) {
            selection = 3;
            date = 0;
        } else if(date + DateUtilities.ONE_WEEK == dueDay.getMillis()) {
            selection = 4;
            date = 0;
        }

        updateSpinnerOptions(date);

        super.readFromTask(task);
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_visibility_off_24dp;
    }

    @Override
    protected void readFromTaskOnInitialize() {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setSelection(selection);
        refreshDisplayView();
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        if(adapter == null || spinner == null) {
            return;
        }
        HideUntilValue item = adapter.getItem(spinner.getSelectedItemPosition());
        if(item == null) {
            return;
        }
        long value = task.createHideUntil(item.setting, item.date);
        task.setHideUntil(value);
    }
}
