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
package com.todoroo.astrid.sharing;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.facebook.android.AuthListener;
import com.facebook.android.Facebook;
import com.facebook.android.LoginButton;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TaskService;

/**
 * This activity allows users to sign in or log in to Producteev
 *
 * @author arne.jans
 *
 */
public class SharingLoginActivity extends Activity implements AuthListener {

    public static final String APP_ID = "169904866369148"; //$NON-NLS-1$

    @Autowired TaskService taskService;

    private Facebook facebook;
    private TextView errors;

    // --- ui initialization

    static {
        AstridDependencyInjector.initialize();
    }

    public String EXTRA_TASK_ID = "task"; //$NON-NLS-1$

    public SharingLoginActivity() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    @SuppressWarnings("nls")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);

        setContentView(R.layout.sharing_login_activity);
        setTitle(R.string.sharing_SLA_title);

        long taskId = getIntent().getLongExtra(EXTRA_TASK_ID, 4L);
        Task task = taskService.fetchById(taskId, Task.TITLE);

        TextView taskInfo = (TextView) findViewById(R.id.taskInfo);
        taskInfo.setText(taskInfo.getText() + "\n\n" + task.getValue(Task.TITLE));

        facebook = new Facebook(APP_ID);

        errors = (TextView) findViewById(R.id.error);
        LoginButton loginButton = (LoginButton) findViewById(R.id.fb_login);
        loginButton.init(this, facebook, this, new String[] {
                "email",
                "offline_access",
                "publish_stream"
        });
    }

    // --- facebook handler

    public void onFBAuthSucceed() {
        System.err.println("GOTCHA SUCCESS! " + facebook.getAccessToken());
        errors.setVisibility(View.VISIBLE);
    }

    public void onFBAuthFail(String error) {
        System.err.println("GOTCHA ERROR: " + error);
        DialogUtilities.okDialog(this, getString(R.string.sharing_SLA_title),
                android.R.drawable.ic_dialog_alert, error, null);
    }

    @Override
    public void onFBAuthCancel() {
        System.err.println("GOTCHA CANCEL");
        // do nothing
    }

    // --- my astrid handler

    /**
     * Create user account via FB
     */
    public void createUserAccountFB() {
        String accessToken = facebook.getAccessToken();

    }



}