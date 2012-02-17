package com.todoroo.astrid.taskrabbit;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItem;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.OAuthLoginActivity;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.taskrabbit.TaskRabbitLocationManager.LocationResult;
import com.todoroo.astrid.ui.FragmentPopover;
import com.todoroo.astrid.welcome.HelpInfoPopover;

public class TaskRabbitActivity extends FragmentActivity {


    public interface TaskRabbitSetListener {
        public void readFromModel(JSONObject json, String key, int mode);
        public void saveToDatabase(JSONObject json, String key) throws JSONException;
        public void postToTaskRabbit(JSONObject json, String key) throws JSONException;
    }
    public interface ActivityResultSetListener {

        public boolean activityResult (int requestCode, int resultCode, Intent data);
    }

    /** task model */
    Task model = null;

    @Autowired
    private RestClient restClient;

    @Autowired
    private Database database;

    @Autowired
    private TaskService taskService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ExceptionService exceptionService;



    /** true if editing started with a new task */
    boolean isNewTask = false;
    private EditText taskTitle;
    private Button  taskButton;
    private LinearLayout taskControls;
    private LinearLayout descriptionControls;
    private Location currentLocation;
    private FragmentPopover menuPopover;
    private TextView menuTitle;
    private ListView menuList;
    private ArrayAdapter<String> adapter;
    private TaskRabbitLocationManager locationManager;


    private int currentSelectedItem = 0;
    private View menuNav;
    private ImageView menuNavDisclosure;

    private final List<TaskRabbitSetListener> controls = Collections.synchronizedList(new ArrayList<TaskRabbitSetListener>());

    private LinearLayout row;

    public static final int REQUEST_CODE_TASK_RABBIT_OAUTH = 5;
    public static final int REQUEST_CODE_ENABLE_GPS = 6;


    public static final int MESSAGE_CODE_SUCCESS = 1;
    public static final int MESSAGE_CODE_FAILURE = -1;
    public static final int MESSAGE_CODE_INTERNET_FAILURE = -2;
    public static final int MESSAGE_CODE_LOCATION_FAILURE = -3;
    /** Act.fm current user name */

    public static final String TASK_RABBIT_TOKEN = "task_rabbit_token"; //$NON-NLS-1$

    private static final String TASK_RABBIT_POPOVER_PREF = "task_rabbit_popover"; //$NON-NLS-1$
    private static final String TASK_RABBIT_DIALOG_INTRO_PREF = "task_rabbit_popover"; //$NON-NLS-1$
    private static final String TASK_RABBIT_PREF_CITY_NAME = "task_rabbit_city_name"; //$NON-NLS-1$
    private static final String TASK_RABBIT_PREF_CITY_ID = "task_rabbit_city_id"; //$NON-NLS-1$
    private static final String TASK_RABBIT_PREF_CITY_LAT = "task_rabbit_city_lat"; //$NON-NLS-1$
    private static final String TASK_RABBIT_PREF_CITY_LNG = "task_rabbit_city_lng"; //$NON-NLS-1$


    public static final String CITY_NAME = "name"; //$NON-NLS-1$
    public static final String CITY_LAT = "lat"; //$NON-NLS-1$
    public static final String CITY_LNG = "lng"; //$NON-NLS-1$
    public static final String LOCATION_CONTAINER = "other_locations_attributes"; //$NON-NLS-1$

    // Non-production values
    public static final String TASK_RABBIT_URL = "http://www.taskrabbit.com"; //$NON-NLS-1$
    public static final String TASK_RABBIT_CLIENT_ID = "RZUDrMuGn9Q3dXeq4nL24bM6LZmMCi1CEGgfP4ND"; //$NON-NLS-1$
    public static final String TASK_RABBIT_CLIENT_APPLICATION_ID = "Va7FUIUTprsmyuwAq9eHSZvAgiRj8FVH1zeaM8Zt"; //$NON-NLS-1$
    //    public static final String TASK_RABBIT_URL = "http://rs-astrid-api.taskrabbit.com"; //$NON-NLS-1$
    //    public static final String TASK_RABBIT_CLIENT_ID = "fDTmGeR0uNCvoxopNyqsRWae8xOvbOBqC7jmHaxv"; //$NON-NLS-1$
    //    public static final String TASK_RABBIT_CLIENT_APPLICATION_ID = "XBpKshU8utH5eaNmhky9N8aAId5rSLTh04Hi60Co"; //$NON-NLS-1$
    public static final String TASK_RABBIT_ID = "id"; //$NON-NLS-1$
    private TaskRabbitTaskContainer taskRabbitTask;

    /* From tag settings */
    private boolean isDialog;


    public TaskRabbitActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setupForDialogOrFullscreen();
        super.onCreate(savedInstanceState);
        loadLocation();
        setContentView(R.layout.task_rabbit_enter);

        StatisticsService.reportEvent(StatisticsConstants.TASK_RABBIT_VIEW);
    }

    public void showAddListPopover() {
        if (!Preferences.getBoolean(TASK_RABBIT_POPOVER_PREF, false)) {
            ActionBar actionBar = getSupportActionBar();
            HelpInfoPopover.showPopover(this, actionBar.getCustomView().findViewById(R.id.menu_nav), R.string.help_popover_taskrabbit_type, null);
            Preferences.setBoolean(TASK_RABBIT_POPOVER_PREF, true);
        }
    }

    public void showIntroDialog() {
        if (!Preferences.getBoolean(TASK_RABBIT_DIALOG_INTRO_PREF, false)) {
            if (TaskRabbitLocationManager.supportsCurrentLocation(currentLocation)) {

                AlertDialog.Builder adb = new AlertDialog.Builder(TaskRabbitActivity.this);
                adb.setTitle(R.string.tr_alert_intro_title);
                adb.setMessage(getString(R.string.tr_alert_intro_location));
                adb.setPositiveButton(getString(R.string.tr_alert_button_close), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,  final int id) {
                        showAddListPopover();
                    }
                });
                adb.show();
            }
            else {
                final AlertDialog adb = new AlertDialog.Builder(TaskRabbitActivity.this)
                .setTitle(R.string.tr_alert_intro_title)
                .setMessage(R.string.tr_alert_intro_no_location)
                .setPositiveButton(getString(R.string.tr_alert_button_close),null)
                .show();

                ((TextView) adb.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            }
            Preferences.setBoolean(TASK_RABBIT_DIALOG_INTRO_PREF, true);
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        StatisticsService.sessionStart(this);
        populateFields();
        showIntroDialog();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            taskRabbitTask.setLocalTaskData(serializeToJSON().toString());
            TaskRabbitDataService.getInstance().saveTaskAndMetadata(taskRabbitTask);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    protected void populateFields() {
        loadItem(getIntent());
        taskRabbitTask = TaskRabbitDataService.getInstance().getContainerForTask(model);
        setUpUIComponents();
    }

    /*
     * ======================================================================
     * =============================================== model reading / saving
     * ======================================================================
     */

    /**
     * Loads action item from the given intent
     *
     * @param intent
     */
    @SuppressWarnings("nls")
    protected void loadItem(Intent intent) {
        if (model != null) {
            // came from bundle
            isNewTask = (model.getValue(Task.TITLE).length() == 0);
            return;
        }

        long idParam = intent.getLongExtra(TaskEditFragment.TOKEN_ID, -1L);

        database.openForReading();
        if (idParam > -1L) {
            model = taskService.fetchById(idParam, Task.PROPERTIES);
            if (model == null) {
                this.onBackPressed();
            }
        }

        // not found by id or was never passed an id
        if (model == null) {
            String valuesAsString = intent.getStringExtra(TaskEditFragment.TOKEN_VALUES);
            ContentValues values = null;
            try {
                if (valuesAsString != null)
                    values = AndroidUtilities.contentValuesFromSerializedString(valuesAsString);
            } catch (Exception e) {
                // oops, can't serialize
            }
            model = TaskService.createWithValues(values, null,
                    taskService, metadataService);
        }

        if (model == null) {
            exceptionService.reportError("task-edit-no-task",
                    new NullPointerException("model"));
            onBackPressed();
            return;
        }

        // clear notification
        Notifications.cancelNotifications(model.getId());

    }


    private void setupForDialogOrFullscreen() {
        //        isDialog = AndroidUtilities.isTabletSized(this);
        //        if (isDialog)
        //            setTheme(ThemeService.getDialogTheme());
        //        else
        ThemeService.applyTheme(this);
    }


    private void setUpControls() {
        TypedArray arrays = getResources().obtainTypedArray(R.array.tr_default_set);
        TypedArray arrayType = getResources().obtainTypedArray(R.array.tr_default_array);
        for (int i = 0; i < arrays.length(); i++) {

            int titleID = arrays.getResourceId(i, 0);
            int arrayID = arrayType.getResourceId(i, 0);
            if (arrayID == R.string.tr_set_key_location) {
                TaskRabbitLocationControlSet set = new TaskRabbitLocationControlSet(this, R.layout.task_rabbit_row, titleID, i);
                set.location = currentLocation;
                controls.add(set);
            }
            else if(arrayID == R.string.tr_set_key_deadline) {

                TaskRabbitDeadlineControlSet deadlineControl = new TaskRabbitDeadlineControlSet(
                        this, R.layout.control_set_deadline,
                        R.layout.task_rabbit_row);
                controls.add(deadlineControl);
                deadlineControl.readFromTask(model);
            }
            else if(arrayID == R.string.tr_set_key_name) {
                TaskRabbitNameControlSet nameControlSet = new TaskRabbitNameControlSet(this,
                        R.layout.control_set_notes, R.layout.task_rabbit_row, titleID);
                controls.add(nameControlSet);
            }
            else  if(arrayID == R.string.tr_set_key_description) {
                TaskRabbitNameControlSet descriptionControlSet = new TaskRabbitNameControlSet(this,
                        R.layout.control_set_notes, R.layout.task_rabbit_row_description, titleID);
                try {
                    descriptionControlSet.readFromModel(new JSONObject().put(getString(arrayID), model.getValue(Task.NOTES)), getString(arrayID), currentSelectedItem);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                controls.add(descriptionControlSet);
            }
            else {
                TaskRabbitSpinnerControlSet set = new TaskRabbitSpinnerControlSet(this, R.layout.task_rabbit_spinner, titleID, i);
                controls.add(set);
            }
        }
        if(TextUtils.isEmpty(taskTitle.getText())) {
            taskTitle.setText(model.getValue(Task.TITLE));
        }
        populateFields(taskRabbitTask);

        displayViewsForMode(currentSelectedItem);
    }
    private void displayViewsForMode(int mode) {


        taskControls.removeAllViews();
        descriptionControls.removeAllViews();

        if (row == null) {
            row = new LinearLayout(this);
            row.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
            row.setOrientation(LinearLayout.HORIZONTAL);
        }
        else {
            row.removeAllViews();
        }

        if (menuTitle != null) {
            menuTitle.setText(getResources().getStringArray(R.array.tr_preset_types)[mode]);
        }
        int[] presetValues = getPresetValues(mode);
        TypedArray keys = getResources().obtainTypedArray(R.array.tr_default_set_key);
        JSONObject parameters = defaultValuesToJSON(keys, presetValues);
        for (int i = 0; i < controls.size(); i++) {
            if (presetValues[i] == -1) continue;
            TaskRabbitSetListener set = controls.get(i);
            int arrayID = keys.getResourceId(i, 0);
            if (arrayID == R.string.tr_set_key_cost_in_cents || arrayID == R.string.tr_set_key_named_price) {
                if(row.getParent() == null)
                    taskControls.addView(row);
                else {
                    //                    View separator = getLayoutInflater().inflate(R.layout.tea_separator, row);
                    //                    separator.setLayoutParams(new LayoutParams(1, LayoutParams.FILL_PARENT));

                }
                LinearLayout displayRow = (LinearLayout)((TaskEditControlSet)set).getDisplayView();
                LinearLayout.LayoutParams layoutParams= null;
                if(arrayID == R.string.tr_set_key_named_price) {
                    layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 1);
                    displayRow.findViewById(R.id.display_row_body).setPadding(5, 0, 10, 0);
                    displayRow.setMinimumWidth(130);
                }
                else {
                    layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 1);
                    displayRow.findViewById(R.id.display_row_body).setPadding(10, 0, 5, 0);
                }
                row.addView(displayRow, layoutParams);
            }
            else if (arrayID == R.string.tr_set_key_description) {
                descriptionControls.addView(((TaskEditControlSet)set).getDisplayView());
            }
            else {
                taskControls.addView(((TaskEditControlSet)set).getDisplayView());
            }
            ((TaskRabbitSetListener) set).readFromModel(parameters, getString(arrayID), currentSelectedItem);
        }
    }
    private JSONObject defaultValuesToJSON (TypedArray keys, int[] presetValues) {

        JSONObject parameters = new JSONObject();
        for(int i = 0; i < keys.length(); i++) {
            try {
                int arrayID = keys.getResourceId(i, 0);
                parameters.put(getString(arrayID), (presetValues[i]));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return parameters;
    }

    /** Initialize UI components */
    private void setUpUIComponents() {
        if (taskTitle == null){



            taskTitle = (EditText) findViewById(R.id.task_title);

            taskControls = (LinearLayout) findViewById(R.id.task_controls);
            descriptionControls = (LinearLayout)findViewById(R.id.description_controls);
            taskButton = (Button) findViewById(R.id.task_button);
            taskButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    submitTaskRabbit();
                }
            });



            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setDisplayShowCustomEnabled(true);
                actionBar.setCustomView(R.layout.task_rabbit_header_view);


                View customView = actionBar.getCustomView();
                menuNav = customView.findViewById(R.id.menu_nav);
                menuNavDisclosure = (ImageView) customView.findViewById(R.id.menu_disclosure_arrow);

                menuTitle = (TextView) customView.findViewById(R.id.task_rabbit_title);
                menuNav.setOnClickListener(menuClickListener);
                createMenuPopover();
            }




            setUpControls();
        }
    }



    private void populateFields(TaskRabbitTaskContainer container) {
        if (container == null) {
            return;
        }

        if(taskRabbitTask.getTaskID() != TaskRabbitTaskContainer.NO_ID) {
            taskButton.setText(getString(R.string.tr_button_already_posted));
            taskButton.setEnabled(false);
        }
        else {
            taskButton.setEnabled(true);
        }

        JSONObject jsonData = container.getLocalTaskData();
        synchronized (controls) {
            if(jsonData != null) {
                String[] keys = getResources().getStringArray(R.array.tr_default_set_key);

                currentSelectedItem = jsonData.optInt(getString(R.string.tr_set_key_type));
                String title = jsonData.optString(getString(R.string.tr_set_key_name));
                if (!TextUtils.isEmpty(title)) {
                    taskTitle.setText(title);
                }
                for (int i = 0; i < controls.size(); i++) {
                    TaskRabbitSetListener set = (TaskRabbitSetListener) controls.get(i);
                    set.readFromModel(jsonData, keys[i], currentSelectedItem);
                }
            }
        }
    }


    /* saving/converting task rabbit data */
    private JSONObject localParamsToJSON () throws JSONException {

        JSONObject parameters = new JSONObject();

        int[] presetValues = getPresetValues(currentSelectedItem);
        String[] keys = getResources().getStringArray(R.array.tr_default_set_key);


        if (menuTitle != null) {
            String descriptionKey = getString(R.string.tr_set_key_description);
            String category = String.format("Category: %s\n", menuTitle.getText().toString());  //$NON-NLS-1$
            parameters.put(descriptionKey, category);
        }
        for (int i = 0; i < controls.size(); i++) {
            if (presetValues[i] == -1) continue;
            TaskRabbitSetListener set = controls.get(i);
            set.postToTaskRabbit(parameters, keys[i]);
        }
        if (parameters.optJSONArray(LOCATION_CONTAINER) == null) {
            parameters.put(getString(R.string.tr_attr_city_id),  Preferences.getInt(TASK_RABBIT_PREF_CITY_ID, 1));
            parameters.put(getString(R.string.tr_attr_city_lat), true);
        }
        parameters.put(getString(R.string.tr_set_key_name), taskTitle.getText().toString());
        parameters.put(getString(R.string.tr_set_key_name), taskTitle.getText().toString());

        return new JSONObject().put("task", parameters);  //$NON-NLS-1$
    }


    private int[] getPresetValues(int mode) {
        TypedArray arrays = getResources().obtainTypedArray(R.array.tr_default_type_array);
        int[] presetValues = getResources().getIntArray(arrays.getResourceId(mode, 0));
        return presetValues;
    }


    private String serializeToJSON () throws JSONException {

        JSONObject parameters = new JSONObject();
        String[] keys = getResources().getStringArray(R.array.tr_default_set_key);
        for (int i = 0; i < controls.size(); i++) {
            TaskRabbitSetListener set = controls.get(i);
            set.saveToDatabase(parameters, keys[i]);
        }
        parameters.put(getString(R.string.tr_set_key_type), currentSelectedItem);
        parameters.put(getString(R.string.tr_set_key_name), taskTitle.getText().toString());
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


    private ProgressDialog progressDialog;

    @SuppressWarnings("nls")
    protected void submitTaskRabbit(){

        if(!Preferences.isSet(TASK_RABBIT_TOKEN)){
            loginTaskRabbit();
        }
        else if (!supportsSelectedLocation()){
            Message successMessage = new Message();
            successMessage.what = MESSAGE_CODE_LOCATION_FAILURE;
            handler.sendMessage(successMessage);
        }
        else {


            if(progressDialog == null)
                progressDialog = DialogUtilities.progressDialog(this,
                        getString(R.string.DLG_please_wait));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean success = false;


                    String urlCall = "tasks/";
                    if (taskRabbitTask.getTaskID() > 0) urlCall += taskRabbitTask.getTaskID();
                    urlCall += String.format("?client_id=%s&client_application=%s", TASK_RABBIT_CLIENT_ID, TASK_RABBIT_CLIENT_APPLICATION_ID);
                    Header authorization = new BasicHeader("Authorization", "OAuth " + Preferences.getStringValue(TASK_RABBIT_TOKEN));
                    Header contentType = new BasicHeader("Content-Type",  "application/json");
                    HttpEntity taskBody = getTaskBody();
                    String response = null;

                    try {
                        response = restClient.post(taskRabbitURL(urlCall), taskBody, contentType, authorization);
                        Log.e("The response", "The post response: " + response);
                        JSONObject taskResponse = new JSONObject(response);
                        if(taskResponse.has(TASK_RABBIT_ID)){
                            taskRabbitTask.setRemoteTaskData(response);
                            taskRabbitTask.setTaskID(taskResponse.optString(TASK_RABBIT_ID));
                            Message successMessage = new Message();
                            successMessage.what = MESSAGE_CODE_SUCCESS;
                            handler.sendMessage(successMessage);
                            success = true;
                        }
                    }
                    catch (UnknownHostException e) {
                        Message failureMessage = new Message();
                        failureMessage.what = MESSAGE_CODE_INTERNET_FAILURE;
                        handler.sendMessage(failureMessage);
                    }
                    catch (Exception e){
                        Message failureMessage = new Message();
                        failureMessage.what = MESSAGE_CODE_FAILURE;
                        handler.sendMessage(failureMessage);
                    }
                    finally {
                        StatisticsService.reportEvent(StatisticsConstants.TASK_RABBIT_POST, "success", new Boolean(success).toString());
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (progressDialog != null) {
                                    DialogUtilities.dismissDialog(TaskRabbitActivity.this, progressDialog);
                                }
                            }
                        });
                    }
                }
            }).start();

        }
        try {
            taskRabbitTask.setLocalTaskData(serializeToJSON().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //submit!
    }


    private final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            String alertMessage = null;
            switch (msg.what) {
            case MESSAGE_CODE_FAILURE:
            {
                alertMessage = getString(R.string.tr_alert_message_fail);
                break;
            }
            case MESSAGE_CODE_INTERNET_FAILURE:
            {
                alertMessage = getString(R.string.tr_alert_internet_message_fail);
                break;
            }
            case MESSAGE_CODE_LOCATION_FAILURE:
            {
                final AlertDialog adb = new AlertDialog.Builder(TaskRabbitActivity.this)
                .setMessage(R.string.tr_alert_location_not_supported_message)
                .setPositiveButton(getString(R.string.tr_alert_button_close),null)
                .show();

                ((TextView) adb.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                return;
            }
            case MESSAGE_CODE_SUCCESS:
            {
                TaskRabbitDataService.getInstance().saveTaskAndMetadata(taskRabbitTask);

                Intent data = new Intent();
                data.putExtra(TaskRabbitControlSet.DATA_RESPONSE, taskRabbitTask.getRemoteTaskData().toString());
                TaskRabbitActivity.this.setResult(Activity.RESULT_OK, data);
                TaskRabbitActivity.this.finish();
                return;
            }
            }

            AlertDialog.Builder adb = new AlertDialog.Builder(TaskRabbitActivity.this);
            adb.setTitle(getString(R.string.tr_alert_title_fail));
            adb.setMessage(alertMessage);
            adb.setPositiveButton(getString(R.string.tr_alert_button_close),null);
            adb.show();
        }
    };


    /* login methods */
    protected void loginTaskRabbit() {
        Intent intent = new Intent(this,
                OAuthLoginActivity.class);
        try {
            String url = TASK_RABBIT_URL + "/api/authorize?client_id=" + TASK_RABBIT_CLIENT_ID;  //$NON-NLS-1$
            intent.putExtra(OAuthLoginActivity.URL_TOKEN, url);
            this.startActivityForResult(intent, REQUEST_CODE_TASK_RABBIT_OAUTH);
            StatisticsService.reportEvent(StatisticsConstants.ACTFM_LOGIN_GL_START);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void loadLocation() {
        if (locationManager == null) {
            locationManager = new TaskRabbitLocationManager(this);
            if ( !locationManager.isLocationUpdatesEnabled()) {
                buildAlertMessageNoGps();
            }
        }
        currentLocation = locationManager.getLastKnownLocation();
        if (currentLocation == null) {
            locationManager.getLocation(new LocationResult(){
                @Override
                public void gotLocation(final Location location){
                    //Got the location!
                    currentLocation = location;
                    updateControlSetLocation(currentLocation);
                }
            }
            );
        }
        else {
            updateControlSetLocation(currentLocation);
        }
    }


    @SuppressWarnings("nls")
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.tr_alert_gps_title))
        .setCancelable(false)
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog,  final int id) {
                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_CODE_ENABLE_GPS);
            }
        })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int id) {
                dialog.cancel();
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    protected void saveUserInfo(String response) throws Exception {
        JSONObject userObject = new JSONObject(response);
        JSONObject cityObject = userObject.getJSONObject("city");  //$NON-NLS-1$
        if (cityObject.has(CITY_NAME)){
            Preferences.setString(TASK_RABBIT_PREF_CITY_NAME, cityObject.getString(CITY_NAME));
        }
        if (cityObject.has(TASK_RABBIT_ID)){
            Preferences.setInt(TASK_RABBIT_PREF_CITY_ID, cityObject.getInt(TASK_RABBIT_ID));
        }
        if (cityObject.has(CITY_LAT)){
            Preferences.setString(TASK_RABBIT_PREF_CITY_LAT, String.valueOf(cityObject.getDouble(CITY_LAT)));
        }
        if (cityObject.has(CITY_LNG)){
            Preferences.setString(TASK_RABBIT_PREF_CITY_LNG, String.valueOf(cityObject.getDouble(CITY_LNG)));
        }
    }

    private String taskRabbitURL(String method) {
        return TASK_RABBIT_URL + "/api/v1/"+ method;  //$NON-NLS-1$

    }


    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TASK_RABBIT_OAUTH && resultCode == Activity.RESULT_OK){
            String result = data.getStringExtra(OAuthLoginActivity.DATA_RESPONSE);

            String key = "access_token=";  //$NON-NLS-1$
            if(result.contains(key)) {
                try {

                    result = result.substring(result.indexOf(key)+key.length());
                    Preferences.setString(TASK_RABBIT_TOKEN, result);
                    String response = restClient.get(taskRabbitURL("account"));   //$NON-NLS-1$
                    saveUserInfo(response);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                return;
            }
        }
        else if (requestCode == REQUEST_CODE_ENABLE_GPS) {

            loadLocation();
        }

        else {
            for (TaskRabbitSetListener set : controls) {
                if (set instanceof ActivityResultSetListener) {
                    if (((ActivityResultSetListener) set).activityResult(requestCode, resultCode, data))
                        return;
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        }
        return super.onOptionsItemSelected(item);
    }




    /* location calls */
    private boolean supportsSelectedLocation() {
            for (TaskRabbitSetListener controlSet : controls) {
                if (TaskRabbitLocationControlSet.class.isAssignableFrom(controlSet.getClass())) {
                    TaskRabbitLocationControlSet locationControlSet = (TaskRabbitLocationControlSet) controlSet;
                    if(!TaskRabbitLocationManager.supportsCurrentLocation(locationControlSet.location) && locationControlSet.getDisplayView().getParent() != null) {
                        return false;
                    }
                }
            }
            return true;
    }

    public void updateControlSetLocation (Location location) {
        for (TaskRabbitSetListener controlSet : controls) {
            if (TaskRabbitLocationControlSet.class.isAssignableFrom(controlSet.getClass())) {
                ((TaskRabbitLocationControlSet) controlSet).updateCurrentLocation(location);
            }
        }
    }

    public boolean isLoggedIn() {
        return !TextUtils.isEmpty(Preferences.getStringValue(TASK_RABBIT_TOKEN));
    }



    /* Menu Popover */
    private void setMenuDropdownSelected(boolean selected) {
        int oldTextColor = menuTitle.getTextColors().getDefaultColor();
        int textStyle = (selected ? R.style.TextAppearance_ActionBar_ListsHeader_Selected :
            R.style.TextAppearance_ActionBar_ListsHeader);

        TypedValue listDisclosure = new TypedValue();
        getTheme().resolveAttribute(R.attr.asListsDisclosure, listDisclosure, false);
        menuTitle.setTextAppearance(this, textStyle);
        menuNav.setBackgroundColor(selected ? oldTextColor : android.R.color.transparent);
        menuNavDisclosure.setSelected(selected);
    }


    private final OnClickListener menuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setMenuDropdownSelected(true);
            menuPopover.show(v);
        }
    };


    private void createMenuPopover() {
        menuPopover = new FragmentPopover(this, R.layout.task_rabbit_menu_popover);
        menuPopover.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                setMenuDropdownSelected(false);
            }
        });
        setupListView();
        menuPopover.setContent(menuList);
    }


    private void setupListView() {
        String[] keys = getResources().getStringArray(R.array.tr_preset_types);
        if (adapter == null) {
            adapter = new ArrayAdapter<String>(this, R.layout.task_rabbit_menu_row, keys);
            menuList = new ListView(this);
            menuList.setAdapter(adapter);
            menuList.setCacheColorHint(Color.TRANSPARENT);

            menuList.setSelection(0);
            menuList.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                        long id) {
                    currentSelectedItem = position;
                    displayViewsForMode(position);
                    menuPopover.dismiss();
                }
            });
        }
        else {
            adapter = new ArrayAdapter<String>(this, R.layout.task_rabbit_menu_row, keys);
            menuList.setAdapter(adapter);
        }
    }



}
