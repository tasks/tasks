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
package com.todoroo.astrid.activity;

import android.content.Intent;
import android.os.Bundle;

import com.timsu.astrid.utilities.StartupReceiver;
import com.todoroo.andlib.service.ExceptionService.TodorooUncaughtExceptionHandler;
import com.todoroo.astrid.filters.FilterExposer;

/**
 * HomeActivity is the primary activity for Astrid and determines which activity
 * to launch next.
 * <p>
 * If the user is completely new, it launches {@link IntroductionActivity}.
 * <p>
 * If the user needs to sign in, it launches {@link SignInActivity}.
 * <p>
 * If the user has no coaches nd no tasks, it launches
 * {@link CoachSelectorActivity}
 * <p>
 * Otherwise, it launches {@link TaskListActivity}.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HomeActivity extends AstridActivity {

    public static final int REQUEST_SIGN_IN = 1;
    public static final int REQUEST_INTRODUCTION = 2;

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    /** Called when loading up the activity */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new TodorooUncaughtExceptionHandler());

        // open controllers & perform application startup rituals
        StartupReceiver.onStartupApplication(this);

        performRedirection();
    }

    /* ======================================================================
     * ======================================================= event handlers
     * ====================================================================== */

    /**
     * Perform redirection to next activity
     */
    private void performRedirection() {
        Intent intent = new Intent(this, TaskListActivity.class);
        intent.putExtra(TaskListActivity.TOKEN_FILTER, FilterExposer.buildInboxFilter(getResources()));
        startActivity(intent);
        finish();
    }

}