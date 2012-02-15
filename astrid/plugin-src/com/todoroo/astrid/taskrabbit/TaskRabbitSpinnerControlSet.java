package com.todoroo.astrid.taskrabbit;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.taskrabbit.TaskRabbitActivity.TaskRabbitSetListener;

public class TaskRabbitSpinnerControlSet extends TaskEditControlSet implements TaskRabbitSetListener{

    private final Spinner spinner;
    private final int titleID;
    private final TextView displayText;
    private final TextView displayEdit;
    private ArrayAdapter<String> adapter;
    private final Activity activity;

    public  TaskRabbitSpinnerControlSet(final Activity activity, int viewLayout, int title, int setID) {
        super(activity, viewLayout);
        this.titleID = title;
        this.activity = activity;
        //        DependencyInjectionService.getInstance().inject(this);

//        spinner = new Spinner(fragment.activity);
        spinner = (Spinner) getDisplayView().findViewById(R.id.spinner);
        spinner.setPrompt(activity.getString(title));

        displayEdit = (TextView) getDisplayView().findViewById(R.id.display_row_edit);

        displayText = (TextView) getDisplayView().findViewById(R.id.display_row_title);
        displayText.setText(title);



        String[] listTypes = getStringDefaultArray(setID, R.array.tr_default_array);



        adapter = new ArrayAdapter<String>(
                activity, android.R.layout.simple_spinner_item, listTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setAdapter(adapter);
            }
        });

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {

                displayEdit.setText(getDisplayEditText());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //
            }
        });
        getView().setOnClickListener(getDisplayClickListener());

    }


    protected OnClickListener getDisplayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                spinner.performClick();
            }
        };
    }
    public String[] getStringDefaultArray(int position, int arrayType) {
            TypedArray arrays = activity.getResources().obtainTypedArray(arrayType);
            int arrayID = arrays.getResourceId(position, -1);
             return activity.getResources().getStringArray(arrayID);
    }
    public void resetAdapter(String[] items) {
        if (adapter == null) return;
        adapter = new ArrayAdapter<String>(
                activity, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setAdapter(adapter);
                spinner.setSelection(0); // plus 1 for the no selection item
            }
        });

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                displayEdit.setText(getDisplayEditText());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //
            }
        });
    }

    public int selectedPosition() {
        return spinner.getSelectedItemPosition();
    }

    @Override
    public void readFromModel(JSONObject json, String key, int mode) {

        if (titleID == R.string.tr_set_named_price){
            String[] listTypes = getStringDefaultArray(mode, R.array.tr_default_price_type_array);
            resetAdapter(listTypes);
        }
        else if (titleID == R.string.tr_set_skill_required){
            String[] listTypes = getStringDefaultArray(mode, R.array.tr_default_skill_type_array);
            resetAdapter(listTypes);
        }
        int intValue = json.optInt(key, 0);

        if (intValue < spinner.getCount()) {
            spinner.setSelection(intValue);
            displayEdit.setText(getDisplayEditText());
        }
    }

    private String getDisplayEditText() {
        if(titleID == R.string.tr_set_cost_in_cents) {
            return activity.getResources().getStringArray(R.array.tr_default_cost_in_cents_display)[spinner.getSelectedItemPosition()];
        }
        else {
            return spinner.getSelectedItem().toString();
        }
    }
    @Override
    public void saveToDatabase(JSONObject json, String key) throws JSONException {
        json.put(key, spinner.getSelectedItemPosition());

    }

    public int parseToDollars (String conversion){
        int index = conversion.lastIndexOf('$');
        String cents = conversion.substring(index+1);
        if(TextUtils.isEmpty(cents)) return 0;
        return Integer.parseInt(cents);
    }


    @Override
    @SuppressWarnings("nls")
    public void postToTaskRabbit(JSONObject json, String key) throws JSONException {

        if(spinner.getSelectedItem() != null){
            String spinnerString = spinner.getSelectedItem().toString();
            if (titleID == R.string.tr_set_cost_in_cents) {
                int cents = parseToDollars(spinnerString) *100;
                json.put(key, cents);
            }
            else if (titleID == R.string.tr_set_named_price) {
                json.put(key,  parseToDollars(spinnerString));
            }
            else if (key.contains("description")) {
                String description = json.optString("description", "");
                description += String.format("\n%S %S", key, spinner.getSelectedItem().toString());
            }
            else {
                json.put(key, spinnerString);
            }
        }
    }


    @Override
    public void readFromTask(Task task) {
        return;
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
    protected void afterInflate() {
        // Nothing, we don't lazy load this control set yet
    }

    @Override
    public String writeToModel(Task task) {
        return null;
    }

}
