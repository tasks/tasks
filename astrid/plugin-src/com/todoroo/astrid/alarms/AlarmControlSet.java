package com.todoroo.astrid.alarms;

import java.util.Date;
import java.util.LinkedHashSet;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.widget.DateControlSet;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class AlarmControlSet implements TaskEditControlSet {

    // --- instance variables

    private final LinearLayout alertsContainer;
    private final Activity activity;

    public AlarmControlSet(Activity activity, ViewGroup parent) {
        View v = LayoutInflater.from(activity).inflate(R.layout.alarm_control, parent, true);

        this.activity = activity;
        this.alertsContainer = (LinearLayout) v.findViewById(R.id.alert_container);
        v.findViewById(R.id.alarms_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                addAlarm(new Date());
            }
        });
    }

    @Override
    public void readFromTask(Task task) {
        alertsContainer.removeAllViews();
        TodorooCursor<Metadata> cursor = AlarmService.getInstance().getAlarms(task.getId());
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
                addAlarm(new Date(cursor.get(AlarmFields.TIME)));
        } finally {
            cursor.close();
        }
    }

    @Override
    public String writeToModel(Task task) {
        LinkedHashSet<Long> alarms = new LinkedHashSet<Long>();
        for(int i = 0; i < alertsContainer.getChildCount(); i++) {
            DateControlSet set = (DateControlSet) alertsContainer.getChildAt(i).getTag();
            if(set == null)
                continue;
            Date date = set.getDate();
            if(date != null)
                alarms.add(set.getDate().getTime());
        }

        if(AlarmService.getInstance().synchronizeAlarms(task.getId(), alarms))
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());

        return null;
    }

    private boolean addAlarm(Date alert) {
        final View alertItem = LayoutInflater.from(activity).inflate(R.layout.alarm_edit_row, null);
        alertsContainer.addView(alertItem);

        DateControlSet dcs = new DateControlSet(activity, (Button)alertItem.findViewById(R.id.date),
                (Button)alertItem.findViewById(R.id.time));
        dcs.setDate(alert);
        alertItem.setTag(dcs);

        ImageButton reminderRemoveButton;
        reminderRemoveButton = (ImageButton)alertItem.findViewById(R.id.button1);
        reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                alertsContainer.removeView(alertItem);
            }
        });

        return true;
    }
}
