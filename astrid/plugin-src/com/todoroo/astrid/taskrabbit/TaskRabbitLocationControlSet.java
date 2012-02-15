package com.todoroo.astrid.taskrabbit;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.taskrabbit.TaskRabbitActivity.ActivityResultSetListener;
import com.todoroo.astrid.taskrabbit.TaskRabbitActivity.TaskRabbitSetListener;

public class TaskRabbitLocationControlSet extends TaskEditControlSet implements TaskRabbitSetListener, ActivityResultSetListener {

    private final TextView displayText;
    private final TextView displayEdit;
    private final Activity activity;
    private String locationName;
    public Location location;
    public JSONObject manualEntry = null;

    public int REQUEST_CODE_TASK_RABBIT_LOCATION = 6;

    public  TaskRabbitLocationControlSet(final Activity activity , int viewLayout, int title, int setID) {
        super(activity, viewLayout);
        this.activity = activity;
        REQUEST_CODE_TASK_RABBIT_LOCATION += setID;

        displayText = (TextView) getDisplayView().findViewById(R.id.display_row_title);
        displayText.setText(activity.getString(title));

        displayEdit = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        displayEdit.setText(getLocationText());

        this.getView().setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    Class.forName("com.google.android.maps.MapView");  //$NON-NLS-1$
                    Intent mapIntent = new Intent(activity, TaskRabbitMapActivity.class);
                    activity.startActivityForResult(mapIntent, REQUEST_CODE_TASK_RABBIT_LOCATION);
                } catch (Exception e) {
                    manualLocationEntry();
                }
            }
        });

    }

    @SuppressWarnings("nls")
    protected void manualLocationEntry() {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        final String[] fields = new String[] { "Name","Address", "City", "State", "Zip" };
        final HashMap<String, EditText> views = new HashMap<String, EditText>();
        for(String field : fields) {
            EditText et = new EditText(activity);
            et.setHint(field);
            et.setLayoutParams(lp);
            views.put(field, et);
            layout.addView(et);
        }

        DialogUtilities.viewDialog(activity, "Enter Location", layout, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                displayEdit.setText(views.get("Address").getText());
                manualEntry = new JSONObject();
                for(String field : fields)
                    try {
                        manualEntry.put(field.toLowerCase(), views.get(field).getText());
                    } catch (JSONException e) {
                        // fail
                    }
            }
        }, null);
    }

    private void parseTaskLocation(JSONObject json) {

        if (json == null) return;
        if (json.has(TaskRabbitActivity.CITY_NAME))
            locationName = json.optString(TaskRabbitActivity.CITY_NAME);
        if(json.has(TaskRabbitActivity.CITY_LNG)) {
            location = new Location(""); //$NON-NLS-1$
            location.setLongitude(json.optInt(TaskRabbitActivity.CITY_LNG));
            location.setLatitude(json.optInt(TaskRabbitActivity.CITY_LAT));
        }
    }

    public String getLocationText () {
        if (!TextUtils.isEmpty(locationName))
            return locationName;

        return activity.getString(R.string.tr_default_location_name);
    }
    public boolean activityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TASK_RABBIT_LOCATION && resultCode == Activity.RESULT_OK) {
            int lng = data.getIntExtra(TaskRabbitActivity.CITY_LNG, 0);
            int lat = data.getIntExtra(TaskRabbitActivity.CITY_LAT, 0);
            locationName = data.getStringExtra(TaskRabbitActivity.CITY_NAME);
            location = new Location("");  //$NON-NLS-1$
            location.setLatitude(locationToDouble(lat));
            location.setLongitude(locationToDouble(lng));
            displayEdit.setText(getLocationText());
            manualEntry = null;


            return true;
        }
        return false;
    }


    public static double locationToDouble(int location) {
        return (location+ 0.0) / 1e6;
    }
    public static int locationToInt(double location) {
        return  (int)(location * 1e6);
    }


    @Override
    public void readFromModel(JSONObject json, String key, int mode) {
            parseTaskLocation(json.optJSONObject(key));
        displayEdit.setText(getLocationText());
    }

    public void updateCurrentLocation(Location currentLocation) {
        if (location == null) {
            location = currentLocation;
        }
    }

    @Override
    public void saveToDatabase(JSONObject json, String key) throws JSONException {
        json.put(key, getTaskLocation());
    }

    @Override
    public void postToTaskRabbit(JSONObject json, String key) throws JSONException {
        JSONArray locations = json.optJSONArray(TaskRabbitActivity.LOCATION_CONTAINER);
        if (locations == null) {
            locations = new JSONArray();
        }
        locations.put(getTaskLocation());
        json.put(TaskRabbitActivity.LOCATION_CONTAINER, locations);

    }

    private JSONObject getTaskLocation() {
        if(manualEntry != null)
            return manualEntry;

        try {
            JSONObject locationObject = new JSONObject();
            if(!TextUtils.isEmpty(locationName)){
                locationObject.put(TaskRabbitActivity.CITY_NAME, locationName);
            }
            else {
                locationObject.put(TaskRabbitActivity.CITY_NAME, displayText.getText().toString());
            }
            if(location != null) {
                locationObject.put(TaskRabbitActivity.CITY_LNG, location.getLongitude());
                locationObject.put(TaskRabbitActivity.CITY_LAT, location.getLatitude());
            }
            return locationObject;
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    //don't need these
    @Override
    public void readFromTask(Task task) {
        //
    }

    @Override
    protected void afterInflate() {
        // Nothing, we don't lazy load this control set yet
    }

    @Override
    protected void readFromTaskOnInitialize() {
        // Nothing, we don't lazy load this control set yet
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        // Nothing, we don't lazy load this control set yet
        return null;
    }

    @Override
    public String writeToModel(Task task) {
        return null;
    }


}
