package com.todoroo.astrid.taskrabbit;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.PopupControlSet;

public class TaskRabbitRow extends PopupControlSet{

    private final int setID;
    private final int type;
    private final TextView body;

    public TaskRabbitRow(Activity activity, int viewLayout, int displayViewLayout, int setID, int type, final String title) {
        super(activity, viewLayout, displayViewLayout, title);
        this.setID = setID;
        this.type = type;
        body = (TextView) getDisplayView().findViewById(R.id.display_row_edit);


    }


    public void setBody(String desc) {
        body.setText(desc);
    }

    public void showPopupForRow() {
        return;
    }
    @Override
    protected void refreshDisplayView() {
        // TODO Auto-generated method stub

    }

    public boolean populateFromTask (TaskRabbitTaskContainer task) {

        if (task != null) {
            String[] types = activity.getResources().getStringArray(R.array.tr_default_set_key);

            JSONObject trJSON = task.getLocalTaskData();
            try {
                trJSON.getString(types[setID]);
                body.setText(trJSON.getString(types[setID]));
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return false;

    }
    public void readFromTask(TaskRabbitTaskContainer task) {
        if (!populateFromTask(task)) {
            return;
        }
        body.setText("Whats up dowg");

    }
    @Override
    public String writeToModel(Task task) {
        return body.getText().toString();
    }


    @Override
    public void readFromTask(Task task) {
        // TODO Auto-generated method stub
    }

}
