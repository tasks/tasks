package com.todoroo.astrid.taskrabbit;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.timsu.astrid.data.location.GeoPoint;
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


    private static final int RADIUS_250_MILES = 400000;

    public interface TaskRabbitSetListener {
        public void readFromModel(JSONObject json, String key);
        public void saveToJSON(JSONObject json, String key) throws JSONException;
        public void writeToJSON(JSONObject json, String key) throws JSONException;
    }
    public interface ActivityResultSetListener {

        public boolean activityResult (int requestCode, int resultCode, Intent data);
    }

    /** task model */
    private Task model = null;

    @Autowired
    private RestClient restClient;

    private Location currentLocation;
    public boolean isEnabledForTRLocation = false;
    public static final String LOCATION_ENABLED = "location_enabled"; //$NON-NLS-1$

    private static final GeoPoint[] supportedLocations = {
            new GeoPoint(42358430, -71059770), //
            new GeoPoint(37739230, -122439880),
            new GeoPoint(40714350, -74005970),
            new GeoPoint(41878110, -8762980),
            new GeoPoint(34052230, -118243680),
            new GeoPoint(33717470, -117831140)
    };

    private final Fragment fragment;
    protected final TextView displayText;
    private LocationManager locationManager;

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
    protected void readFromTaskPrivate() {
        // Nothing, we don't lazy load this control set yet
    }

    @Override
    protected String writeToModelPrivate(Task task) {
        // Nothing, we don't lazy load this control set yet
        return null;
    }

    @Override
    protected void afterInflate() {
        // Nothing, we don't lazy load this control set yet
    }

    @Override
    public String writeToModel(Task task) {
        return null;
    }


    private void updateTaskRow(TaskRabbitTaskContainer container) {
        displayText.setText(fragment.getString(R.string.tr_display_status));
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
        Toast.makeText(fragment.getActivity(), fragment.getString(R.string.tr_success_toast),
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

    private String taskRabbitURL(String method) {
        return String.format("%s/api/v1/%s?client_id=%s&client_application=%s", TaskRabbitActivity.TASK_RABBIT_URL, method, TaskRabbitActivity.TASK_RABBIT_CLIENT_ID, TaskRabbitActivity.TASK_RABBIT_CLIENT_APPLICATION_ID);  //$NON-NLS-1$
    }

    /** Fire task rabbit if assigned **/
    @Override
    public boolean showTaskRabbitForUser(String name, JSONObject json) {
        if (name.equals(fragment.getActivity().getString(R.string.actfm_EPA_task_rabbit))) {
            showTaskRabbitActivity();
            return true;
        }
        return false;
    }

    public boolean activityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TASK_RABBIT_ACTIVITY && resultCode == Activity.RESULT_OK){
            String result = data.getStringExtra(OAuthLoginActivity.DATA_RESPONSE);
            if (!TextUtils.isEmpty(result)) {
                try {
                    Message successMessage = new Message();
                    successMessage.what = 1;
                    handler.sendMessageDelayed(successMessage, 500);
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

        final long taskID = json.optLong(TaskRabbitActivity.TASK_RABBIT_ID);
        if (taskID == TaskRabbitTaskContainer.NO_ID) return;
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    String response = restClient.get(taskRabbitURL("tasks/" + taskID)); //$NON-NLS-1$
                    JSONObject taskResponse = new JSONObject(response);
                    if(taskResponse.has(TaskRabbitActivity.TASK_RABBIT_ID)){
                        taskRabbitTask.setRemoteTaskData(response);
                        taskRabbitTask.setTaskID(taskResponse.optString(TaskRabbitActivity.TASK_RABBIT_ID));
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
        locationManager = (LocationManager) fragment.getActivity().getSystemService(Context.LOCATION_SERVICE);
        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (currentLocation == null) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            } catch (IllegalArgumentException e) {
                // No gps
                isEnabledForTRLocation = false;
            }
        } else {
            isEnabledForTRLocation = supportsCurrentLocation(currentLocation);
        }
    }

    public static boolean supportsCurrentLocation(Location location) {
        if (location == null) return false;
        for (GeoPoint point : supportedLocations){
            Location city = new Location("");  //$NON-NLS-1$
            city.setLatitude(point.getLatitudeE6()/1E6);
            city.setLongitude(point.getLongitudeE6()/1E6);
            float distance = location.distanceTo(city);
            if (distance < RADIUS_250_MILES) {
                return true;
            }
        }
        return false;
    }




    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        isEnabledForTRLocation = supportsCurrentLocation(currentLocation);
        locationManager.removeUpdates(this);
        locationManager = null;

    }

    @Override
    public void onProviderDisabled(String provider) {
        //

    }

    @Override
    public void onProviderEnabled(String provider) {
        //

    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //

    }

    @Override
    public boolean didPostToTaskRabbit() {
        if (taskRabbitTask == null) return false;
        return taskRabbitTask.getTaskID() != TaskRabbitTaskContainer.NO_ID;
    }


}
