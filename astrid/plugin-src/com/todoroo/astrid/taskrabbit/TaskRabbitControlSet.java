package com.todoroo.astrid.taskrabbit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.ui.PopupControlSet;

public class TaskRabbitControlSet extends PopupControlSet implements AssignedChangedListener, LocationListener {


    public interface TaskRabbitSetListener {
        public void readFromModel(JSONObject json, String key);
        public void saveToJSON(JSONObject json, String key) throws JSONException;
        public void writeToJSON(JSONObject json, String key) throws JSONException;
    }

    /** task model */
    Task model = null;

    @Autowired
    private RestClient restClient;


    GeoPoint[] supportedLocations =
    {
            new GeoPoint(42358430, -71059770),
            new GeoPoint(37739230, -122439880),
            new GeoPoint(40714350, -74005970),
            new GeoPoint(41878110, -8762980),
            new GeoPoint(34052230, -118243680),
            new GeoPoint(33717470, -117831140)};

    /** true if editing started with a new task */
    boolean isNewTask = false;
    private EditText taskDescription;
    private Button  taskButton;
    private LinearLayout taskControls;
    private Location currentLocation;
    private final Fragment fragment;
    private final List<TaskRabbitSetListener> controls = Collections.synchronizedList(new ArrayList<TaskRabbitSetListener>());

    private Spinner spinnerMode;

    public static final int REQUEST_CODE_TASK_RABBIT_OAUTH = 5;
    /** Act.fm current user name */

    public static final String TASK_RABBIT_TOKEN = "task_rabbit_token"; //$NON-NLS-1$
    //public static final String TASK_RABBIT_URL = "http://www.taskrabbit.com"; //$NON-NLS-1$
    public static final String TASK_RABBIT_URL = "http://rs-astrid-api.taskrabbit.com"; //$NON-NLS-1$
    public static final String TASK_RABBIT_CLIENT_ID = "fDTmGeR0uNCvoxopNyqsRWae8xOvbOBqC7jmHaxv"; //$NON-NLS-1$
    public static final String TASK_RABBIT_CLIENT_APPLICATION_ID = "XBpKshU8utH5eaNmhky9N8aAId5rSLTh04Hi60Co"; //$NON-NLS-1$

    public static final String CITY_NAME = "task_rabbit_city_name"; //$NON-NLS-1$
    private TaskRabbitTaskContainer taskRabbitTask;



    public TaskRabbitControlSet(Fragment fragment, int viewLayout, int displayViewLayout) {
        super(fragment.getActivity(), viewLayout, displayViewLayout, 0);
        this.fragment = fragment;
        DependencyInjectionService.getInstance().inject(this);
        loadLocation();

    }


    @Override
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

    @Override
    protected OnClickListener getDisplayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpUIComponents();
                dialog.show();
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


    private void setUpControls() {
        TypedArray arrays = activity.getResources().obtainTypedArray(R.array.tr_default_set);
        TypedArray arrayType = activity.getResources().obtainTypedArray(R.array.tr_default_array);
        for (int i = 0; i < arrays.length(); i++) {


            int titleID = arrays.getResourceId(i, -1);
            int arrayID = arrayType.getResourceId(i, -1);
            if (arrayID == R.string.tr_location) {
                TaskRabbitLocationControlSet set = new TaskRabbitLocationControlSet(fragment, R.layout.task_rabbit_row, titleID, i, spinnerMode.getSelectedItemPosition());
                controls.add(set);
            }
            else if(arrayID == R.string.tr_deadline) {

                TaskRabbitDeadlineControlSet deadlineControl = new TaskRabbitDeadlineControlSet(
                        activity, R.layout.control_set_deadline,
                        R.layout.task_rabbit_row);
                controls.add(deadlineControl);
            }
            else if(arrayID == R.string.tr_name) {
                TaskRabbitNameControlSet nameControlSet = new TaskRabbitNameControlSet(activity,
                        R.layout.control_set_notes, R.layout.control_set_notes_display, titleID, i);
                controls.add(nameControlSet);
            }
            else {
                TaskRabbitSpinnerControlSet set = new TaskRabbitSpinnerControlSet(fragment, R.layout.task_rabbit_spinner, titleID, i, spinnerMode.getSelectedItemPosition());
                controls.add(set);
            }
        }
        if(taskDescription.getText().length() == 0){
            taskDescription.setText(model.getValue(Task.TITLE) + model.getValue(Task.NOTES));
        }
        populateFields(taskRabbitTask);
    }
    private void displayViewsForMode(int mode) {

        for (int i = 0; i < taskControls.getChildCount(); i++){
            ViewGroup row = (ViewGroup)taskControls.getChildAt(i);
            row.removeAllViews();
        }
        taskControls.removeAllViews();

        LinearLayout row = null;

        int[] presetValues = getPresetValues(mode);
        String[] keys = activity.getResources().getStringArray(R.array.tr_default_set_key);
        for (int i = 1; i < controls.size(); i++) {
            if (presetValues[i] == -1) continue;
            if (row == null || row.getChildCount() == 2) {
                row = new LinearLayout(activity);
                row.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 100));
                row.setOrientation(LinearLayout.HORIZONTAL);
                taskControls.addView(row);
            }
            TaskRabbitSetListener set = controls.get(i);
            row.addView(((TaskEditControlSet)set).getDisplayView(),  new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
            JSONObject parameters = new JSONObject();
            try {
                parameters.put(keys[i], (presetValues[i]));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ((TaskRabbitSetListener) set).readFromModel(parameters, keys[i]);
        }
    }

    /** Initialize UI components */
    private void setUpUIComponents() {
        if (taskDescription == null){
            taskDescription = (EditText) getView().findViewById(R.id.task_description);

            taskControls = (LinearLayout)getView().findViewById(R.id.task_controls);

            taskButton = (Button) getView().findViewById(R.id.task_button);
            taskButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    submitTaskRabbit();
                }
            });



            spinnerMode = (Spinner) getView().findViewById(R.id.task_type);
            String[] listTypes = activity.getResources().getStringArray(
                    R.array.tr_preset_types);

            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    activity, android.R.layout.simple_spinner_item, listTypes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    spinnerMode.setAdapter(adapter);
                }
            });
            spinnerMode.setOnItemSelectedListener(new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                        int arg2, long arg3) {
                    displayViewsForMode(spinnerMode.getSelectedItemPosition());
                }
                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    return;
                }
            });


            setUpControls();
        }
    }

    private void populateFields(TaskRabbitTaskContainer container) {
        if (container == null) {
            return;
        }
        JSONObject jsonData = container.getLocalTaskData();
        synchronized (controls) {
            if(jsonData != null) {
                String[] keys = activity.getResources().getStringArray(R.array.tr_default_set_key);
                spinnerMode.setSelection(jsonData.optInt(keys[0]));
                for (int i = 1; i < controls.size(); i++) {
                    TaskRabbitSetListener set = (TaskRabbitSetListener) controls.get(i);
                    set.readFromModel(jsonData, keys[i]);
                }
            }
        }
    }

    private JSONObject localParamsToJSON () throws JSONException {

        JSONObject parameters = new JSONObject();

        int[] presetValues = getPresetValues(spinnerMode.getSelectedItemPosition());
        String[] keys = activity.getResources().getStringArray(R.array.tr_default_set_key);

        parameters.put(activity.getString(R.string.tr_set_key_type), spinnerMode.getSelectedItem().toString());
        parameters.put(activity.getString(R.string.tr_set_key_description), taskDescription.getText().toString());
        for (int i = 1; i < controls.size(); i++) {
            if (presetValues[i] == -1) continue;
            TaskRabbitSetListener set = controls.get(i);
            set.writeToJSON(parameters, keys[i]);
        }
        if (parameters.optJSONArray("other_locations_attributes") == null) {
            parameters.put(activity.getString(R.string.tr_attr_city_id),  Preferences.getInt("task_rabbit_city_id", 1));
            parameters.put(activity.getString(R.string.tr_attr_city_lat), true);
        }

        Log.d("localParamsToJSON", parameters.toString());
        return new JSONObject().put("task", parameters);
    }


    private int[] getPresetValues(int mode) {
        TypedArray arrays = activity.getResources().obtainTypedArray(R.array.tr_default_type_array);
        int[] presetValues = activity.getResources().getIntArray(arrays.getResourceId(mode, -1));
        return presetValues;
    }


    private String serializeToJSON () throws JSONException {

        JSONObject parameters = new JSONObject();
        String[] keys = activity.getResources().getStringArray(R.array.tr_default_set_key);
        for (int i = 1; i < controls.size(); i++) {
            TaskRabbitSetListener set = controls.get(i);
            set.saveToJSON(parameters, keys[i]);
        }
        parameters.put(activity.getString(R.string.tr_set_key_type), spinnerMode.getSelectedItemPosition());
        parameters.put(activity.getString(R.string.tr_set_key_description), taskDescription.getText().toString());
        return parameters.toString();
    }
    private HttpEntity getTaskBody()  {

        try {
            return new StringEntity(localParamsToJSON().toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    private final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case -1:

                if(dialog.isShowing()) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(activity);
                    adb.setTitle("Error posting task");
                    adb.setMessage("Please try again");
                    adb.setPositiveButton("Close",null);
                    adb.show();
                }
                break;
            case 0: break;
            case 1:
                showSuccessToast();
                TaskRabbitDataService.getInstance().saveTaskAndMetadata(taskRabbitTask);
                updateDisplay(taskRabbitTask.getRemoteTaskData());
                dialog.dismiss();
                break;
            case 2:
                TaskRabbitDataService.getInstance().saveTaskAndMetadata(taskRabbitTask);
                updateDisplay(taskRabbitTask.getRemoteTaskData());
                dialog.dismiss();
                break;
            }
        }
    };

    protected void submitTaskRabbit(){

        if(!Preferences.isSet(TASK_RABBIT_TOKEN)){
            loginTaskRabbit();
        }
        else {


            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        String urlCall = "tasks/";
                        Log.d("Tasks url:", taskRabbitURL(urlCall));
                        Header authorization = new BasicHeader("Authorization", "OAuth " + Preferences.getStringValue(TASK_RABBIT_TOKEN));  //$NON-NLS-1$
                        Header contentType = new BasicHeader("Content-Type",  //$NON-NLS-1$
                        "application/json");   //$NON-NLS-1$
                        String response = restClient.post(taskRabbitURL(urlCall), getTaskBody(), contentType, authorization);
                        Log.d("Task rabbit response", response);
                        JSONObject taskResponse = new JSONObject(response);
                        if(taskResponse.has("id")){
                            taskRabbitTask.setRemoteTaskData(response);
                            taskRabbitTask.setTaskID(taskResponse.optString("id"));
                            Message successMessage = new Message();
                            successMessage.what = 1;
                            handler.sendMessage(successMessage);
                        }

                    }
                    catch (Exception e){
                        e.printStackTrace();
                        Message failureMessage = new Message();
                        failureMessage.what = -1;
                        handler.sendMessage(failureMessage);
                    }
                }
            }).start();

        }
        try {
            taskRabbitTask.setLocalTaskData(serializeToJSON().toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //submit!
    }

    /**
     * Show toast for task edit canceling
     */
    private void showSuccessToast() {
        Toast.makeText(activity, "Task posted to Task Rabbit successfully!",
                Toast.LENGTH_SHORT).show();
    }


    protected void loginTaskRabbit() {
        Intent intent = new Intent(activity,
                OAuthLoginActivity.class);
        try {
            String url = TASK_RABBIT_URL + "/api/authorize?client_id=" + TASK_RABBIT_CLIENT_ID;
            intent.putExtra(OAuthLoginActivity.URL_TOKEN, url);
            fragment.startActivityForResult(intent, REQUEST_CODE_TASK_RABBIT_OAUTH);
            StatisticsService.reportEvent(StatisticsConstants.ACTFM_LOGIN_GL_START);
        } catch (Exception e) {
            //            handleError(e);
            e.printStackTrace();
        }
    }
    private void loadLocation() {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 500.0f, this);
        updateControlSetLocation(currentLocation);
    }

    protected void saveUserInfo(String response) throws Exception {
        JSONObject userObject = new JSONObject(response);
        JSONObject cityObject = userObject.getJSONObject("city");
        if (cityObject.has("name")){
            Preferences.setString("task_rabbit_city_name", cityObject.getString("name"));
        }
        if (cityObject.has("id")){
            Preferences.setInt("task_rabbit_city_id", cityObject.getInt("id"));
        }
        if (cityObject.has("lat")){
            //            currentLocation.setLatitude(cityObject.getDouble("lat"));
            Preferences.setString("task_rabbit_city_lat", String.valueOf(cityObject.getDouble("lat")));
        }
        if (cityObject.has("lng")){
            //            currentLocation.setLongitude(cityObject.getDouble("lng"));
            Preferences.setString("task_rabbit_city_lng", String.valueOf(cityObject.getDouble("lng")));
        }
    }

    private String taskRabbitURL(String method) {
        return TASK_RABBIT_URL + "/api/v1/"+ method;

    }

    /** Fire task rabbit if assigned **/
    @Override
    public void assignedChanged(String name, JSONObject json) {
        // TODO Auto-generated method stub
        if (name.equals(activity.getString(R.string.actfm_EPA_task_rabbit))) {
            setUpUIComponents();
            dialog.show();
        }
    }

    public boolean activityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TASK_RABBIT_OAUTH && resultCode == Activity.RESULT_OK){
            String result = data.getStringExtra(OAuthLoginActivity.DATA_RESPONSE);
            if(result.contains("access_token=")) {
                try {
                    result = result.substring(result.indexOf("access_token=")+"access_token=".length());
                    Preferences.setString(TASK_RABBIT_TOKEN, result);
                    String response = restClient.get(taskRabbitURL("account"));
                    Log.d("Task rabbit response", response);
                    saveUserInfo(response);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            return true;
        }
        else {
            for (TaskRabbitSetListener set : controls) {
                if (set.getClass().equals(TaskRabbitLocationControlSet.class)) {
                    if (((TaskRabbitLocationControlSet) set).activityResult(requestCode, resultCode, data)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /*
     * (non-Javadoc)
     * @see android.location.LocationListener#onLocationChanged(android.location.Location)
     */
    @Override
    public void onLocationChanged(Location location) {

        currentLocation = location;
        updateControlSetLocation(currentLocation);

    }
    public void updateControlSetLocation (Location location) {
        for (TaskRabbitSetListener controlSet : controls) {
            if (TaskRabbitLocationControlSet.class.isAssignableFrom(controlSet.getClass())) {
                ((TaskRabbitLocationControlSet) controlSet).updateCurrentLocation(location);
            }
        }
    }
    @Override
    public void onProviderDisabled(String provider) {
        return;
    }
    @Override
    public void onProviderEnabled(String provider) {
        return;
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        return;
    }


    /*
     *
     */

    public void updateDisplay(JSONObject json) {
        if (json != null && json.has("state_label")) {
            String status = json.optString(activity.getString(R.string.tr_attr_state_label));
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
                    Log.d("Tasks url:", taskRabbitURL("tasks/" + taskID));
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
        return !TextUtils.isEmpty(Preferences.getStringValue(TASK_RABBIT_TOKEN));
    }
    @Override
    public boolean shouldShowTaskRabbit() {
        //if (isLoggedIn()) return true;
        if(!Locale.getDefault().getCountry().equals("US") ) return false; //$NON-NLS-1$
        for (GeoPoint point : supportedLocations){
            Location city = new Location(""); //$NON-NLS-1$
            city.setLatitude(point.getLatitudeE6()/1E6);
            city.setLongitude(point.getLongitudeE6()/1E6);
            float distance = currentLocation.distanceTo(city);
            if (distance < 400000) {
                return true;
            }
        }
        return false;
    }
}
