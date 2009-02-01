package com.timsu.astrid.activities;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
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
 * Main activity uses a ViewFlipper to flip between child views.
 * 
 * @author Tim Su (timsu@stanfordalumni.org)
 */
public class TaskList extends Activity {
	
    /**
     * Interface for views that are displayed from the main view page
     * 
     * @author timsu
     */
    abstract public static class SubActivity {
    	private TaskList parent;
    	private ActivityCode code;
    	private View view;
    	
    	public SubActivity(TaskList parent, ActivityCode code, View view) {
			this.parent = parent;
			this.code = code;
			this.view = view;
			view.setTag(this);
		}

    	// --- pass-through to activity listeners
    	
    	/** Called when this subactivity is displayed to the user */
    	void onDisplay(Bundle variables) {
    		//
    	}
    	
    	boolean onPrepareOptionsMenu(Menu menu) {
    		return false;
    	}
    	
    	void onActivityResult(int requestCode, int resultCode, Intent data) {
    		//
    	}
    	
    	boolean onMenuItemSelected(int featureId, MenuItem item) {
    		return false;
    	}
    	
    	
    	void onWindowFocusChanged(boolean hasFocus) {
    		//
    	}
    	
    	boolean onKeyDown(int keyCode, KeyEvent event) {
    		return false;
    	}
    	
    	// --- pass-through to activity methods
    	
    	public Resources getResources() {
    		return parent.getResources();
    	}
    	
    	public View findViewById(int id) {
    		return view.findViewById(id);
    	}
    	
    	public void startManagingCursor(Cursor c) {
    		parent.startManagingCursor(c);
    	}
    	
    	public void setTitle(CharSequence title) {
    		parent.setTitle(title);
    	}
    	
    	public void closeActivity() {
    		parent.finish();
    	}
    	
    	public void launchActivity(Intent intent, int requestCode) {
    		parent.startActivityForResult(intent, requestCode);
    	}
    	
    	// --- helper methods
    	
    	public Activity getParent() {
    		return parent;
    	}
    	
    	public TaskController getTaskController() {
    		return parent.taskController;
    	}
    	
    	public TagController getTagController() {
    		return parent.tagController;
    	}
    	
    	public View.OnTouchListener getGestureListener() {
    		return parent.gestureListener;
    	}
    	
    	public void switchToActivity(ActivityCode activity, Bundle state) {
    		parent.switchToActivity(activity, state);
    	}
    	
    	// --- internal methods
    	
    	protected ActivityCode getActivityCode() {
    		return code;
    	}
    	
    	protected View getView() {
			return view;
		}
    }
    
    /* ======================================================================
     * ======================================================= internal stuff
     * ====================================================================== */
	
    
    public enum ActivityCode {
    	TASK_LIST,
    	TAG_LIST,
    	TASK_LIST_W_TAG
    };
    
    private static final String TAG_LAST_ACTIVITY = "l";
    private static final String TAG_LAST_BUNDLE = "b";
    private static final int FLING_DIST_THRESHOLD = 100;
	private static final int FLING_VEL_THRESHOLD = 300;

	// view components
	private ViewFlipper viewFlipper;
	private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;
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
	private TaskController taskController;
	private TagController tagController;

	// static variables
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
        
        if(savedInstanceState != null && savedInstanceState.containsKey(TAG_LAST_ACTIVITY)) {
        	viewFlipper.setDisplayedChild(savedInstanceState.getInt(TAG_LAST_ACTIVITY));
        	Bundle variables = savedInstanceState.getBundle(TAG_LAST_BUNDLE);
        	getCurrentSubActivity().onDisplay(variables);
        } else {
        	getCurrentSubActivity().onDisplay(null);
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
    
    private void switchToActivity(ActivityCode activity, Bundle variables) {
    	closeOptionsMenu();
    	
    	// and flip to them
    	switch(getCurrentSubActivity().getActivityCode()) {
    	case TASK_LIST:
            viewFlipper.setInAnimation(mInAnimationForward);
            viewFlipper.setOutAnimation(mOutAnimationForward);
            viewFlipper.showNext();
            if(activity == ActivityCode.TASK_LIST_W_TAG)
    			viewFlipper.showNext();
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
            viewFlipper.showPrevious();
            if(activity == ActivityCode.TASK_LIST_W_TAG)
    			viewFlipper.showPrevious();
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
    
    private SubActivity getCurrentSubActivity() {
    	return (SubActivity)viewFlipper.getCurrentView().getTag();
    }
    
    /* ======================================================================
     * ======================================================= event handling
     * ====================================================================== */
   
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TAG_LAST_ACTIVITY, getCurrentSubActivity().code.ordinal());
        outState.putBundle(TAG_LAST_BUNDLE, lastActivityBundle);
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
