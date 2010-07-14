/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.todoroo.astrid.R;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.rmilk.Utilities.ListContainer;
import com.todoroo.astrid.rmilk.data.MilkDataService;

/**
 * Displays a dialog box for users to edit their RTM stuff
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkEditActivity extends Activity {

    long taskId;
    MilkDataService service;
    Task model;

    Spinner list;
    EditText repeat;

    /** Called when loading up the activity */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskId = getIntent().getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        setContentView(R.layout.rmilk_edit_activity);
        setTitle(R.string.rmilk_MEA_title);

        ((Button)findViewById(R.id.ok)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                saveAndQuit();
            }
        });
        ((Button)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        // load all lists
        service = new MilkDataService(this);
        ListContainer[] lists = service.getLists();

        list = (Spinner) findViewById(R.id.rmilk_list);
        ArrayAdapter<ListContainer> listAdapter = new ArrayAdapter<ListContainer>(
                this, android.R.layout.simple_spinner_item,
                lists);
        listAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        list.setAdapter(listAdapter);

        // load model
        model = service.readTask(taskId);
        repeat.setText(model.getValue(MilkDataService.REPEAT));
        list.setSelection(0); // TODO
    }

    /**
     * Save tags to task and then quit
     */
    protected void saveAndQuit() {
        // model.setValue(DataService.LIST_ID, list.getSelectedItem()); TODO

        setResult(RESULT_OK);
        finish();
    }

}