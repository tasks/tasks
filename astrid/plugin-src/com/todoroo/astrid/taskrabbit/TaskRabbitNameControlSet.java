package com.todoroo.astrid.taskrabbit;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;

import com.todoroo.astrid.taskrabbit.TaskRabbitControlSet.TaskRabbitSetListener;
import com.todoroo.astrid.ui.EditNotesControlSet;

public class TaskRabbitNameControlSet extends EditNotesControlSet implements TaskRabbitSetListener{

    public TaskRabbitNameControlSet(Activity activity, int viewLayout,
            int displayViewLayout, int titleID, int i) {
        super(activity, viewLayout, displayViewLayout);
        displayText.setText("Restaurant name");
    }

    @Override
    public void readFromModel(JSONObject json, String key) {

        editText.setTextKeepState(json.optString(key, ""));
        notesPreview.setText(json.optString(key, ""));
    }

    @Override
    public void saveToJSON(JSONObject json, String key) throws JSONException {
        json.put(key, editText.getText().toString());
    }

    @Override
    public void writeToJSON(JSONObject json, String key) throws JSONException {
        json.put(key, editText.getText().toString());

    }


}
