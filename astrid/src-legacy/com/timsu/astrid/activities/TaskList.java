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

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RemoteViews;
import android.widget.ViewFlipper;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.timsu.astrid.appwidget.AstridAppWidgetProvider;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.sync.Synchronizer;
import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.AstridUtilities.AstridUncaughtExceptionHandler;
import com.todoroo.astrid.reminders.StartupReceiver;

/**
 * TaskList is the main launched activity for Astrid. It uses a ViewFlipper
 * to flip between child views, which in this case are the TaskListSubActivity
 * and the TagListSubActivity.
 *
 * @author timsu
 */
public class TaskList extends Activity {

    // constants for the different pages that we can display
    public static final int AC_TASK_LIST = 0;
    public static final int AC_TAG_LIST = 1;
    public static final int AC_TASK_LIST_W_TAG = 2;

    /** Bundle Key: activity code id of current activity */
    private static final String LAST_ACTIVITY_TAG = "l";

    /** Bundle Key: variables of current activity */
    private static final String LAST_BUNDLE_TAG = "b";

    /** Bundle Key: variables to pass to the sub-activity */
    public static final String VARIABLES_TAG = "v";

    /** Minimum distance a fling must cover to trigger motion */
    private static final int FLING_DIST_THRESHOLD = 120;

    /** Maximum distance in the other axis for a fling */
    private static final int MAX_FLING_OTHER_AXIS = 300;

    /** Minimum velocity a fling must have to trigger motion */
	private static final int FLING_VEL_THRESHOLD = 200;

	// view components
	private ViewFlipper viewFlipper;
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;
	private SubActivity taskList;
	private SubActivity tagList;
	private SubActivity taskListWTag;
	private Bundle lastActivityBundle;

	// animations
	private Animation mFadeInAnim;
    private Animation mFadeOutAnim;

	// data controllers
	TaskController taskController;
	TagController tagController;

	// static variables
	public static boolean synchronizeNow = false;

	/** If set, the application will close when this activity gets focus */
	static boolean shouldCloseInstance = false;

    @Override
    /** Called when loading up the activity for the first time */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // set uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new AstridUncaughtExceptionHandler());

        // open controllers & perform application startup rituals
        StartupReceiver.onStartupApplication(this);
        shouldCloseInstance = false;
        taskController = new TaskController(this);
        taskController.open();
        tagController = new TagController(this);
        tagController.open();

        setupUIComponents();

        Bundle variables = new Bundle();

        if(savedInstanceState != null && savedInstanceState.containsKey(LAST_ACTIVITY_TAG)) {
        	viewFlipper.setDisplayedChild(savedInstanceState.getInt(LAST_ACTIVITY_TAG));
        	Bundle lastBundle = savedInstanceState.getBundle(LAST_BUNDLE_TAG);
        	if(lastBundle != null)
        	    variables.putAll(lastBundle);
        }
        if(getIntent().hasExtra(VARIABLES_TAG))
            variables.putAll(getIntent().getBundleExtra(VARIABLES_TAG));

        getCurrentSubActivity().onDisplay(variables);

        // sync now if requested
        if(synchronizeNow) {
        	synchronizeNow = false;
        	Synchronizer sync = new Synchronizer(false);
            sync.setTagController(tagController);
            sync.setTaskController(taskController);
            sync.synchronize(this, null);
        }

        // if we have no filter tag, we're not on the last task
        if(getCurrentSubActivity() == taskListWTag &&
                ((TaskListSubActivity)taskListWTag).getFilterTag() == null) {
            switchToActivity(AC_TASK_LIST, null);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // set up flurry
        FlurryAgent.onStartSession(this, Constants.FLURRY_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateWidget();

        FlurryAgent.onEndSession(this);
    }

    private void updateWidget()
    {
    	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

    	RemoteViews views = AstridAppWidgetProvider.UpdateService.buildUpdate(this);
    	ComponentName widgetName = new ComponentName(this, AstridAppWidgetProvider.class);
    	appWidgetManager.updateAppWidget(widgetName, views);
    }

    /** Set up user interface components */
    private void setupUIComponents() {
        gestureDetector = new GestureDetector(new AstridGestureDetector());
        viewFlipper = (ViewFlipper)findViewById(R.id.main);
        taskList = new TaskListSubActivity(this, AC_TASK_LIST,
        		findViewById(R.id.tasklist_layout));
        tagList = new TagListSubActivity(this, AC_TAG_LIST,
        		findViewById(R.id.taglist_layout));
        taskListWTag = new TaskListSubActivity(this, AC_TASK_LIST_W_TAG,
        		findViewById(R.id.tasklistwtag_layout));

        mFadeInAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        mFadeOutAnim = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        viewFlipper.setInAnimation(mFadeInAnim);
        viewFlipper.setOutAnimation(mFadeOutAnim);

        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };
    }

    /** Gesture detector switches between sub-activities */
    class AstridGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if(Math.abs(e1.getY() - e2.getY()) > MAX_FLING_OTHER_AXIS)
                    return false;

                Log.i("astrid", "Got fling. X: " + (e2.getX() - e1.getX()) +
                        ", vel: " + velocityX + " Y: " + (e2.getY() - e1.getY()));

                // flick R to L
                if(e1.getX() - e2.getX() > FLING_DIST_THRESHOLD &&
                        Math.abs(velocityX) > FLING_VEL_THRESHOLD) {

                	switch(getCurrentSubActivity().getActivityCode()) {
                	case AC_TASK_LIST:
                		switchToActivity(AC_TAG_LIST, null);
                		return true;
            		default:
            			return false;
                	}
                }

                // flick L to R
                else if(e2.getX() - e1.getX() > FLING_DIST_THRESHOLD &&
                        Math.abs(velocityX) > FLING_VEL_THRESHOLD) {

                	switch(getCurrentSubActivity().getActivityCode()) {
                	case AC_TASK_LIST_W_TAG:
                		switchToActivity(AC_TAG_LIST, null);
                		return true;
                	case AC_TAG_LIST:
                		switchToActivity(AC_TASK_LIST, null);
                		return true;
            		default:
            			return false;
                	}
                }
            } catch (Exception e) {
                // ignore!
            }

            return false;
        }
    }

    /* ======================================================================
     * ==================================================== subactivity stuff
     * ====================================================================== */

    /** Switches to another activity, with appropriate animation */
    void switchToActivity(int activity, Bundle variables) {
    	closeOptionsMenu();

    	// and flip to them
    	switch(getCurrentSubActivity().getActivityCode()) {
    	case AC_TASK_LIST:
            switch(activity) {
            case AC_TAG_LIST:
            	viewFlipper.showNext();
            	break;
            case AC_TASK_LIST_W_TAG:
            	viewFlipper.setDisplayedChild(taskListWTag.code);
            }
            break;

    	case AC_TAG_LIST:
    		switch(activity) {
    		case AC_TASK_LIST:
    			viewFlipper.showPrevious();
    			break;
    		case AC_TASK_LIST_W_TAG:
    			viewFlipper.showNext();
    			break;
            }
    		break;

    	case AC_TASK_LIST_W_TAG:
            switch(activity) {
            case AC_TAG_LIST:
            	viewFlipper.showPrevious();
            	break;
            case AC_TASK_LIST:
            	viewFlipper.setDisplayedChild(taskList.code);
            }
            break;
    	}

    	// initialize the components
    	switch(activity) {
    	case AC_TASK_LIST:
    		taskList.onDisplay(variables);
    		break;
    	case AC_TAG_LIST:
    		tagList.onDisplay(variables);
    		break;
    	case AC_TASK_LIST_W_TAG:
    		taskListWTag.onDisplay(variables);
    	}

    	lastActivityBundle = variables;
    }

    /** Helper method gets the currently visible subactivity */
    private SubActivity getCurrentSubActivity() {
    	return (SubActivity)viewFlipper.getCurrentView().getTag();
    }

    /* ======================================================================
     * ======================================================= event handling
     * ====================================================================== */

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LAST_ACTIVITY_TAG, getCurrentSubActivity().code);
        outState.putBundle(LAST_BUNDLE_TAG, lastActivityBundle);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(getCurrentSubActivity().onKeyDown(keyCode, event))
    		return true;
    	else
    		return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	menu.clear();
        return getCurrentSubActivity().onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Constants.RESULT_GO_HOME) {
        	switchToActivity(AC_TASK_LIST, null);
        } else
        	getCurrentSubActivity().onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus && shouldCloseInstance) { // user wants to quit
        	finish();
        } else
        	getCurrentSubActivity().onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	if(getCurrentSubActivity().onMenuItemSelected(featureId, item))
    		return true;
    	else
    		return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event))
	        return true;
	    else
	    	return false;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return getCurrentSubActivity().onRetainNonConfigurationInstance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    	taskController.close();
        tagController.close();
    }
}
