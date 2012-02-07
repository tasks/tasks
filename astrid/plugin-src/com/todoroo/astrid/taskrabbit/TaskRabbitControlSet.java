package com.todoroo.astrid.taskrabbit;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
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

public class TaskRabbitControlSet extends TaskEditControlSet implements AssignedChangedListener {


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
    boolean isNewTask = false;
    private Location currentLocation;

    private final Fragment fragment;
    private LinearLayout row;
    protected final TextView displayText;

    public static final int REQUEST_CODE_TASK_RABBIT_ACTIVITY = 5;
    public static final String DATA_RESPONSE = "response"; //$NON-NLS-1$

    /** Act.fm current user name */

    public static final String TASK_RABBIT_TOKEN = "task_rabbit_token"; //$NON-NLS-1$
    //public static final String TASK_RABBIT_URL = "http://www.taskrabbit.com"; //$NON-NLS-1$
    public static final String TASK_RABBIT_URL = "http://rs-astrid-api.taskrabbit.com"; //$NON-NLS-1$
    public static final String TASK_RABBIT_CLIENT_ID = "fDTmGeR0uNCvoxopNyqsRWae8xOvbOBqC7jmHaxv"; //$NON-NLS-1$
    public static final String TASK_RABBIT_CLIENT_APPLICATION_ID = "XBpKshU8utH5eaNmhky9N8aAId5rSLTh04Hi60Co"; //$NON-NLS-1$

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

    private void showTaskRabbitActivity() {
        Intent intent = new Intent(fragment.getActivity(), TaskRabbitActivity.class);
        intent.putExtra(TaskEditFragment.TOKEN_ID, model.getId());
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
        return TASK_RABBIT_URL + "/api/v1/"+ method;

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
        Log.d("The actiivty result request code", "Rerjwklrw" + REQUEST_CODE_TASK_RABBIT_ACTIVITY);
        if (requestCode == REQUEST_CODE_TASK_RABBIT_ACTIVITY && resultCode == Activity.RESULT_OK){
            String result = data.getStringExtra(OAuthLoginActivity.DATA_RESPONSE);
            if (TextUtils.isEmpty(result)) {
                try {
                    updateDisplay(new JSONObject(result));
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
        if(Locale.getDefault().getCountry().equals("US")) return true; //$NON-NLS-1$
        return false;
    }


}
