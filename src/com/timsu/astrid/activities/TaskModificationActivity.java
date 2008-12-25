package com.timsu.astrid.activities;

import android.app.Activity;
import android.os.Bundle;

import com.timsu.astrid.data.task.AbstractTaskModel;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;

public abstract class TaskModificationActivity<MODEL_TYPE extends
        AbstractTaskModel> extends Activity {
    public static final String LOAD_INSTANCE_TOKEN = "id";
    protected TaskController controller;
    protected MODEL_TYPE model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = new TaskController(this);
        controller.open();

        // check if we have a TaskIdentifier
        TaskIdentifier identifier = null;
        Bundle extras = getIntent().getExtras();
        if(savedInstanceState != null && savedInstanceState.containsKey(LOAD_INSTANCE_TOKEN)) {
            identifier = new TaskIdentifier(savedInstanceState.getLong(
                    LOAD_INSTANCE_TOKEN));
        } else if(extras != null)
            identifier = new TaskIdentifier(extras.getLong(
                    LOAD_INSTANCE_TOKEN));

        model = getModel(identifier);
    }

    abstract protected MODEL_TYPE getModel(TaskIdentifier identifier);

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controller.close();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(model.getTaskIdentifier() != null)
            outState.putLong(LOAD_INSTANCE_TOKEN, model.getTaskIdentifier().getId());
    }
}
