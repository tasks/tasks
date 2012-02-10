package com.todoroo.astrid.taskrabbit;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.EditPeopleControlSet.AssignedChangedListener;
import com.todoroo.astrid.actfm.OAuthLoginActivity;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;

public class TaskRabbitControlSet extends TaskEditControlSet implements AssignedChangedListener, LocationListener {


    public interface TaskRabbitSetListener {
        public void readFromModel(JSONObject json, String key);
        public void saveToJSON(JSONObject json, String key) throws JSONException;
        public void writeToJSON(JSONObject json, String key) throws JSONException;
    }
    public interface ActivityResultSetListener {

        public boolean activityResult (int requestCode, int resultCode, Intent data);
    }

    /** task model */
    Task model = null;

    @Autowired
    private RestClient restClient;


    /** true if editing started with a new task */
    private final boolean isNewTask = false;
    private Location currentLocation;
    public boolean isEnabledForTRLocation = false;
    public static final String LOCATION_ENABLED = "location_enabled"; //$NON-NLS-1$


    GeoPoint[] supportedLocations =
    {
            new GeoPoint(42358430, -71059770), //
            new GeoPoint(37739230, -122439880),
            new GeoPoint(40714350, -74005970),
            new GeoPoint(41878110, -8762980),
            new GeoPoint(34052230, -118243680),
            new GeoPoint(33717470, -117831140)};


    private final Fragment fragment;
    private LinearLayout row;
    protected final TextView displayText;
    LocationManager locationManager;

    public static final int REQUEST_CODE_TASK_RABBIT_ACTIVITY = 5;
    public static final String DATA_RESPONSE = "response"; //$NON-NLS-1$

    /** Act.fm current user name */

    private TaskRabbitTaskContainer taskRabbitTask;


    public TaskRabbitControlSet(Fragment fragment, int displayViewLayout) {
        super(fragment.getActivity(), displayViewLayout);
        this.fragment = fragment;
        DependencyInjectionService.getInstance().inject(this);

        displayText = (TextView) getView().findViewById(R.id.display_row_title);
        if (displayText != null) {
            displayText.setMaxLines(2);
        }

        if (getView() != null) {
            getView().setOnClickListener(getDisplayClickListener());
        }
        loadLocation();
    }






    protected void refreshDisplayView() {
        JSONObject remoteData = taskRabbitTask.getRemoteTaskData();
        if (remoteData != null)
            updateDisplay(remoteData);
    }


    @Override
    public void readFromTask(Task task) {
        model = task;
        taskRabbitTask = TaskRabbitDataService.getInstance().getContainerForTask(model);
        updateTaskRow(taskRabbitTask);

    }

    public void showTaskRabbitActivity() {
        Intent intent = new Intent(fragment.getActivity(), TaskRabbitActivity.class);
        intent.putExtra(TaskEditFragment.TOKEN_ID, model.getId());
        intent.putExtra(LOCATION_ENABLED, isEnabledForTRLocation);
        fragment.startActivityForResult(intent, REQUEST_CODE_TASK_RABBIT_ACTIVITY);
    }
    protected OnClickListener getDisplayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                showTaskRabbitActivity();
            }
        };
    }

    @Override
    public String writeToModel(Task task) {
        //        TaskRabbitDataService.getInstance().saveTaskAndMetadata(taskRabbitTask);
        return null;
    }


    private void updateTaskRow(TaskRabbitTaskContainer container) {
        displayText.setText("Task Rabbit Status");
        JSONObject remoteData = container.getRemoteTaskData();
        if (remoteData != null) {
            updateDisplay(remoteData);
            updateStatus(remoteData);
        }
    }



    /* message callbacks */
    /**
     * Show toast for task edit canceling
     */
    private void showSuccessToast() {
        Toast.makeText(fragment.getActivity(), "Task posted to Task Rabbit successfully!",
                Toast.LENGTH_SHORT).show();
    }

    private final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 0: break;
            case 1:
                showSuccessToast();
                updateDisplay(taskRabbitTask.getRemoteTaskData());
                break;
            }
        }
    };


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setMessage("Yout GPS seems to be disabled, do you want to enable it?")
        .setCancelable(false)
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                fragment.getActivity().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                dialog.cancel();
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    private String taskRabbitURL(String method) {
        return String.format("%S/api/v1/%S?client_id=%S&client_application=%S", TaskRabbitActivity.TASK_RABBIT_URL, method, TaskRabbitActivity.TASK_RABBIT_CLIENT_ID, TaskRabbitActivity.TASK_RABBIT_CLIENT_APPLICATION_ID);
    }

    /** Fire task rabbit if assigned **/
    @Override
    public boolean showTaskRabbitForUser(String name, JSONObject json) {
        // TODO Auto-generated method stub
        if (name.equals(fragment.getActivity().getString(R.string.actfm_EPA_task_rabbit))) {
            showTaskRabbitActivity();
            return true;
        }
        return false;
    }

    public boolean activityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TASK_RABBIT_ACTIVITY && resultCode == Activity.RESULT_OK){
            String result = data.getStringExtra(OAuthLoginActivity.DATA_RESPONSE);
            if (TextUtils.isEmpty(result)) {
                try {
                    Message successMessage = new Message();
                    successMessage.what = 1;
                    handler.sendMessageDelayed(successMessage, 1500);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            return true;
        }
        return false;
    }


    /*
     *
     */

    public void updateDisplay(JSONObject json) {
        String stateKey = fragment.getActivity().getString(R.string.tr_attr_state_label);
        if (json != null && json.has(stateKey)) {
            String status = json.optString(stateKey);
            TextView statusText = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
            statusText.setText(status);
            getDisplayView().setVisibility(View.VISIBLE);
        }
        else {
            getDisplayView().setVisibility(View.GONE);
        }
    }


    protected void updateStatus(JSONObject json){

        final int taskID = json.optInt("id"); //$NON-NLS-1$
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Log.d("Tasks url:", taskRabbitURL("tasks/" + taskID + "?client_id=" + TaskRabbitActivity.TASK_RABBIT_CLIENT_ID));
                    String response = restClient.get(taskRabbitURL("tasks/" + taskID));
                    Log.d("Task rabbit response", response);
                    JSONObject taskResponse = new JSONObject(response);
                    if(taskResponse.has("id")){
                        taskRabbitTask.setRemoteTaskData(response);
                        taskRabbitTask.setTaskID(taskResponse.optString("id"));
                        Message successMessage = new Message();
                        successMessage.what = 2;
                        handler.sendMessage(successMessage);
                    }

                }
                catch (Exception e){
                    e.printStackTrace();
                    Message failureMessage = new Message();
                    failureMessage.what = 0;
                    handler.sendMessage(failureMessage);
                }
            }
        }).start();
        //submit!
    }

    public boolean isLoggedIn() {
        return !TextUtils.isEmpty(Preferences.getStringValue(TaskRabbitActivity.TASK_RABBIT_TOKEN));
    }
    @Override
    public boolean shouldShowTaskRabbit() {
        if(Locale.getDefault().getCountry().equals("US")) return true; //$NON-NLS-1$
        return false;
    }




    private void loadLocation() {
        Log.d("TRControlSet", "gJgHFDSJKGFHSJKFGHDSJKFGSJDGFSDJKFGDSJKFGSHJDFHS:LDFHS:FJKSDJFL:");
        locationManager = (LocationManager) fragment.getActivity().getSystemService(Context.LOCATION_SERVICE);
        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (currentLocation == null) {
            Log.d("TRControlSet", "Fail current location is null");
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
        else {
            Log.d("TRControlSet", "loading location and checking if we suppor it");
            isEnabledForTRLocation = supportsCurrentLocation();
        }
    }

    public boolean supportsCurrentLocation() {

        //TODO test this
        if (currentLocation == null) return false;
        for (GeoPoint point : supportedLocations){
            Log.d("TRControlSet", "Searching if we support current location");
            Location city = new Location(""); //$NON-NLS-1$
            city.setLatitude(point.getLatitudeE6()/1E6);
            city.setLongitude(point.getLongitudeE6()/1E6);
            float distance = currentLocation.distanceTo(city);
            if (distance < 400000) { //250 mi radius
                return true;
            }
        }
        return false;
    }




    @Override
    public void onLocationChanged(Location location) {

        Log.d("TRControlSet", "Location changed and found");
        currentLocation = location;
        isEnabledForTRLocation = supportsCurrentLocation();
        locationManager.removeUpdates(this);
        locationManager = null;

    }
    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }




    @Override
    public boolean didPostToTaskRabbit() {
        if (taskRabbitTask == null) return false;
        return taskRabbitTask.getTaskID() > 0;
    }


}
