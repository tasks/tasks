/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.activities;

import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.timsu.astrid.data.task.AbstractTaskModel;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.utilities.AstridUtilities;
import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.DialogUtilities;

/** Hack hack for tabbed activity. Should provide better interaction
 *
 * @author timsu
 */
public abstract class TaskModificationTabbedActivity<MODEL_TYPE extends
        AbstractTaskModel> extends TabActivity {
    public static final String LOAD_INSTANCE_TOKEN = TaskModificationActivity.LOAD_INSTANCE_TOKEN;

    protected TaskController controller;
    protected MODEL_TYPE model;

    protected class Context {
        protected MODEL_TYPE cModel;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        controller = new TaskController(this);
        controller.open();

        // if we've saved our model, use that instead
        if(getLastNonConfigurationInstance() != null) {
            Context context = (Context)getLastNonConfigurationInstance();
            model = context.cModel;
        } else {
            try {
                // check if we have a TaskIdentifier
                TaskIdentifier identifier = null;
                Bundle extras = getIntent().getExtras();
                if(savedInstanceState != null && savedInstanceState.containsKey(LOAD_INSTANCE_TOKEN)) {
                    identifier = new TaskIdentifier(savedInstanceState.getLong(
                            LOAD_INSTANCE_TOKEN));
                } else if(extras != null && extras.containsKey(LOAD_INSTANCE_TOKEN))
                    identifier = new TaskIdentifier(extras.getLong(
                            LOAD_INSTANCE_TOKEN));

                model = getModel(identifier);
            } catch (Exception e) {
                showErrorAndFinish(R.string.error_opening, e);
            }
        }
    }

    /**
     * Called when screen is rotated. We return the model we are editing so that
     * we will have access to it instead of having to re-read from the database
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        Context context = new Context();
        context.cModel = model;
        return context;
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, Constants.FLURRY_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    protected void showErrorAndFinish(int prefix, Throwable e) {
        AstridUtilities.reportFlurryError("taskedit", e);
        Resources r = getResources();
        DialogUtilities.okDialog(this,
                r.getString(prefix) + " " +
                e.getLocalizedMessage(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
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
