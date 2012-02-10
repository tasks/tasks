package com.todoroo.astrid.taskrabbit;

import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.taskrabbit.TaskRabbitActivity.ActivityResultSetListener;
import com.todoroo.astrid.taskrabbit.TaskRabbitActivity.TaskRabbitSetListener;

public class TaskRabbitLocationControlSet extends TaskEditControlSet implements TaskRabbitSetListener, ActivityResultSetListener {

    private final int setID;
    private final TextView displayText;
    private final TextView displayEdit;
    private final Activity activity;
    private Location location;
    private String locationName;

    public int REQUEST_CODE_TASK_RABBIT_LOCATION = 6;

    public  TaskRabbitLocationControlSet(final Activity activity , int viewLayout, int title, int setID) {
        super(activity, viewLayout);
        this.setID = setID;
        this.activity = activity;
        //        DependencyInjectionService.getInstance().inject(this);
        REQUEST_CODE_TASK_RABBIT_LOCATION += setID;

        displayText = (TextView) getDisplayView().findViewById(R.id.display_row_title);
        displayText.setText(activity.getString(title));

        displayEdit = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        displayEdit.setText(getLocationText());

        this.getView().setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(activity, TaskRabbitMapActivity.class);
                activity.startActivityForResult(mapIntent, REQUEST_CODE_TASK_RABBIT_LOCATION);

            }
        });

    }

    private void parseTaskLocation(JSONObject json) {

        if (json == null) return;
        if (json.has("name"))
            locationName = json.optString("name");
        if(json.has("lng")) {
            location = new Location("");
            location.setLatitude(json.optInt("lng"));
            location.setLatitude(json.optInt("lat"));
        }
    }

    public String getLocationText () {
        if (!TextUtils.isEmpty(locationName))
            return locationName;

        return "Location is set";
    }
    public boolean activityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TASK_RABBIT_LOCATION && resultCode == Activity.RESULT_OK) {
            int lng = data.getIntExtra("lng", 0);
            int lat = data.getIntExtra("lat", 0);
            locationName = data.getStringExtra("name");
            location = new Location("");
            location.setLatitude(locationToDouble(lat));
            location.setLongitude(locationToDouble(lng));
            displayEdit.setText(getLocationText());

            getAddressFromLocation(location);
            Log.d("TASK RABBIT CONTROL SET FOUND CODE", "THE LAT IS: " + lat + " LNG IS: " + lng);

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



    private void getAddressFromLocation(Location location){
        try {
            Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
            // Acquire a reference to the system Location Manager
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null){
                for (Address address : addresses){
                    updateAddress(address);
                }
            }
        } catch (Exception e) {
            Log.d("Location error", e.toString());
        }
    }
    private void updateAddress(Address address){
        if(address.getLocality() != null && address.getPostalCode() != null){
            locationName += "\n"+ (address.getLocality() + ", " + address.getPostalCode());
            //            currentAddress = address;
        }
    }

    @Override
    public void readFromModel(JSONObject json, String key, int mode) {
            parseTaskLocation(json.optJSONObject(key));
        displayEdit.setText(getLocationText());
    }

    public void updateCurrentLocation(Location currentLocation) {
        if (location != null) {
            location = currentLocation;
        }
    }

    @Override
    public void saveToJSON(JSONObject json, String key) throws JSONException {
        json.put(key, getTaskLocation());
        Log.d("LOCATION SAVE", json.toString());
    }

    @Override
    public void writeToJSON(JSONObject json, String key) throws JSONException {
        Log.d("LOCATION", "LOCEIJREGHSK");
        JSONArray locations = json.optJSONArray("other_locations_attributes");
        if (locations == null) {
            locations = new JSONArray();
        }
        locations.put(getTaskLocation());
        json.put("other_locations_attributes", locations);
        Log.d("LOCATION", "LALALALAL" + json.toString());

    }


    private JSONObject getTaskLocation() {

        try {
            JSONObject locationObject = new JSONObject();
            if(!TextUtils.isEmpty(locationName)){
                locationObject.put("name", locationName);
            }
            else {
                locationObject.put("name", displayText.getText().toString());
            }
            if(location != null) {
                locationObject.put("lng", location.getLongitude());
                locationObject.put("lat", location.getLatitude());
            }
            else {
                // TODO FIX THIS DON"T PUT IN ADDRESSES
                locationObject.put("address", "300 Beale");
                locationObject.put("city", "San Francisco");
                locationObject.put("state", "CA");
                locationObject.put("zip", "94158");
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
    }
    @Override
    public String writeToModel(Task task) {
        return null;
    }


}
