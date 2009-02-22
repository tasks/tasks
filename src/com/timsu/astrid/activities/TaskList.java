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

import java.util.Date;

import android.app.Activity;
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
import android.widget.ViewFlipper;

import com.timsu.astrid.R;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.sync.Synchronizer;
import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.Preferences;
import com.timsu.astrid.utilities.StartupReceiver;

/**
 * TaskList is the main launched activity for Astrid. It uses a ViewFlipper
 * to flip between child views, which in this case are the TaskListSubActivity
 * and the TagListSubActivity.
 *
 * @author Tim Su (timsu@stanfordalumni.org)
 */
public class TaskList extends Activity {

    /** Enum for the different pages that we can display */
    public enum ActivityCode {
    	TASK_LIST,
    	TAG_LIST,
    	TASK_LIST_W_TAG
    };

    /** Bundle Key: activity code id of current activity */
    private static final String LAST_ACTIVITY_TAG = "l";

    /** Bundle Key: variables of current activity */
    private static final String LAST_BUNDLE_TAG = "b";

    /** Bundle Key: variables to pass to the subactivity */
    public  static final String VARIABLES_TAG = "v";

    /** Minimum distance a fling must cover to trigger motion */
    private static final int FLING_DIST_THRESHOLD = 70;

    /** Minimum velocity a fling must have to trigger motion */
	private static final int FLING_VEL_THRESHOLD = 250;

	// view components
	private ViewFlipper viewFlipper;
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;
	private SubActivity taskList;
	private SubActivity tagList;
	private SubActivity taskListWTag;
	private Bundle lastActivityBundle;

	// animations
	private Animation mInAnimationForward;
    private Animation mOutAnimationForward;
    private Animation mInAnimationBackward;
    private Animation mOutAnimationBackward;

	// data controllers
	TaskController taskController;
	TagController tagController;

	// static variables

	/** If set, the application will close when this activity gets focus */
	static boolean shouldCloseInstance = false;

    @Override
    /** Called when loading up the activity for the first time */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // open controllers & perform application startup rituals
        StartupReceiver.onStartupApplication(this);
        shouldCloseInstance = false;
        taskController = new TaskController(this);
        taskController.open();
        tagController = new TagController(this);
        tagController.open();
        Synchronizer.setTagController(tagController);
        Synchronizer.setTaskController(taskController);

        setupUIComponents();

        if(savedInstanceState != null && savedInstanceState.containsKey(LAST_ACTIVITY_TAG)) {
        	viewFlipper.setDisplayedChild(savedInstanceState.getInt(LAST_ACTIVITY_TAG));
        	Bundle variables = savedInstanceState.getBundle(LAST_BUNDLE_TAG);
        	getCurrentSubActivity().onDisplay(variables);
        } else {
            Bundle variables = null;
            if(getIntent().hasExtra(VARIABLES_TAG))
                variables = getIntent().getBundleExtra(VARIABLES_TAG);
        	getCurrentSubActivity().onDisplay(variables);
        }

        // auto sync if requested
        Integer autoSyncHours = Preferences.autoSyncFrequency(this);
        if(autoSyncHours != null) {
            final Date lastSync = Preferences.getSyncLastSync(this);

            if(lastSync == null || lastSync.getTime() +
                    1000L*3600*autoSyncHours < System.currentTimeMillis()) {
                Synchronizer.synchronize(this, true, null);
            }
        }

        // if we have no filter tag, we're not on the last task
        if(getCurrentSubActivity() == taskListWTag &&
                ((TaskListSubActivity)taskListWTag).getFilterTag() == null) {
            switchToActivity(ActivityCode.TASK_LIST, null);
        }
    }

    /** Set up user interface components */
    private void setupUIComponents() {
        gestureDetector = new GestureDetector(new AstridGestureDetector());
        viewFlipper = (ViewFlipper)findViewById(R.id.main);
        taskList = new TaskListSubActivity(this, ActivityCode.TASK_LIST,
        		findViewById(R.id.tasklist_layout));
        tagList = new TagListSubActivity(this, ActivityCode.TAG_LIST,
        		findViewById(R.id.taglist_layout));
        taskListWTag = new TaskListSubActivity(this, ActivityCode.TASK_LIST_W_TAG,
        		findViewById(R.id.tasklistwtag_layout));

        mInAnimationForward = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
        mOutAnimationForward = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
        mInAnimationBackward = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
        mOutAnimationBackward = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);

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
    private class AstridGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                Log.i("astrid", "Got fling. X: " + (e2.getX() - e1.getX()) +
                        ", vel: " + velocityX);

                // flick R to L
                if(e1.getX() - e2.getX() > FLING_DIST_THRESHOLD &&
                        Math.abs(velocityX) > FLING_VEL_THRESHOLD) {

                	switch(getCurrentSubActivity().getActivityCode()) {
                	case TASK_LIST:
                		switchToActivity(ActivityCode.TAG_LIST, null);
                		return true;
            		default:
            			return false;
                	}
                }

                // flick L to R
                else if(e2.getX() - e1.getX() > FLING_DIST_THRESHOLD &&
                        Math.abs(velocityX) > FLING_VEL_THRESHOLD) {

                	switch(getCurrentSubActivity().getActivityCode()) {
                	case TASK_LIST_W_TAG:
                		switchToActivity(ActivityCode.TAG_LIST, null);
                		return true;
                	case TAG_LIST:
                		switchToActivity(ActivityCode.TASK_LIST, null);
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
    void switchToActivity(ActivityCode activity, Bundle variables) {
    	closeOptionsMenu();

    	// and flip to them
    	switch(getCurrentSubActivity().getActivityCode()) {
    	case TASK_LIST:
            viewFlipper.setInAnimation(mInAnimationForward);
            viewFlipper.setOutAnimation(mOutAnimationForward);
            switch(activity) {
            case TAG_LIST:
            	viewFlipper.showNext();
            	break;
            case TASK_LIST_W_TAG:
            	viewFlipper.setDisplayedChild(taskListWTag.code.ordinal());
            }
            break;

    	case TAG_LIST:
    		switch(activity) {
    		case TASK_LIST:
    			viewFlipper.setInAnimation(mInAnimationBackward);
    			viewFlipper.setOutAnimation(mOutAnimationBackward);
    			viewFlipper.showPrevious();
    			break;
    		case TASK_LIST_W_TAG:
    			viewFlipper.setInAnimation(mInAnimationForward);
    			viewFlipper.setOutAnimation(mOutAnimationForward);
    			viewFlipper.showNext();
    			break;
            }
    		break;

    	case TASK_LIST_W_TAG:
            viewFlipper.setInAnimation(mInAnimationBackward);
            viewFlipper.setOutAnimation(mOutAnimationBackward);
            switch(activity) {
            case TAG_LIST:
            	viewFlipper.showPrevious();
            	break;
            case TASK_LIST:
            	viewFlipper.setDisplayedChild(taskList.code.ordinal());
            }
            break;
    	}

    	// initialize the components
    	switch(activity) {
    	case TASK_LIST:
    		taskList.onDisplay(variables);
    		break;
    	case TAG_LIST:
    		tagList.onDisplay(variables);
    		break;
    	case TASK_LIST_W_TAG:
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
        outState.putInt(LAST_ACTIVITY_TAG, getCurrentSubActivity().code.ordinal());
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
        	switchToActivity(ActivityCode.TASK_LIST, null);
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
    protected void onDestroy() {
        super.onDestroy();
    	taskController.close();
        tagController.close();
        Synchronizer.setTagController(null);
        Synchronizer.setTaskController(null);
    }
}
