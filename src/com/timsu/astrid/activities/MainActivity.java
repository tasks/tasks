package com.timsu.astrid.activities;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
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
 * Main activity uses a ViewFlipper to flip between child views.
 * 
 * @author Tim Su (timsu@stanfordalumni.org)
 */
public class MainActivity extends Activity {
	
    /**
     * Interface for views that are displayed from the main view page
     * 
     * @author timsu
     */
    abstract public static class SubActivity {
    	private MainActivity parent;
    	private SubActivities code;
    	private View view;
    	
    	public SubActivity(MainActivity parent, SubActivities code, View view) {
			this.parent = parent;
			this.code = code;
			this.view = view;
			view.setTag(this);
		}

    	// --- pass-through to activity listeners
    	
    	abstract void onDisplay(Bundle variables);
    	abstract boolean onPrepareOptionsMenu(Menu menu);
    	abstract void onActivityResult(int requestCode, int resultCode, Intent data);
    	abstract boolean onOptionsItemSelected(MenuItem item);
    	
    	void onWindowFocusChanged(boolean hasFocus) {
    		//
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
    	
    	public void switchToActivity(SubActivities activity, Bundle state) {
    		parent.switchToActivity(activity, state);
    	}
    	
    	// --- internal methods
    	
    	public SubActivities getActivityCode() {
    		return code;
    	}
    }
    
    /* ======================================================================
     * ======================================================= internal stuff
     * ====================================================================== */
	
    public enum SubActivities {
    	TASK_LIST,
    	TAG_LIST,
    	TASK_LIST_W_TAG
    };
    
    public static final int FLING_DIST_THRESHOLD = 100;
	public static final int FLING_VEL_THRESHOLD = 300;

	// view components
	private ViewFlipper viewFlipper;
	private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;
	private SubActivity taskList;
	private SubActivity tagList;
	private SubActivity taskListWTag;
	
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
        getCurrentSubActivity().onDisplay(savedInstanceState);

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
        taskList = new TaskList(this, SubActivities.TASK_LIST, 
        		findViewById(R.id.tasklist_layout));
        tagList = new TagList(this, SubActivities.TAG_LIST, 
        		findViewById(R.id.taglist_layout));
        taskListWTag = new TaskList(this, SubActivities.TASK_LIST_W_TAG, 
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
                		switchToActivity(SubActivities.TAG_LIST, null);
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
                		switchToActivity(SubActivities.TAG_LIST, null);
                		return true;
                	case TAG_LIST:
                		switchToActivity(SubActivities.TASK_LIST, null);
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
    
    private void switchToActivity(SubActivities activity, Bundle variables) {
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
    	
    	// and flip to them
    	switch(getCurrentSubActivity().getActivityCode()) {
    	case TASK_LIST:
            viewFlipper.setInAnimation(mInAnimationForward);
            viewFlipper.setOutAnimation(mOutAnimationForward);
            viewFlipper.showNext();
            if(activity == SubActivities.TASK_LIST_W_TAG)
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
            if(activity == SubActivities.TASK_LIST_W_TAG)
    			viewFlipper.showPrevious();
            break;
    	}
    	
    	viewFlipper.getCurrentView().requestFocus();
    }
    
    private SubActivity getCurrentSubActivity() {
    	return (SubActivity)viewFlipper.getCurrentView().getTag();
    }
    
    /* ======================================================================
     * ======================================================= event handling
     * ====================================================================== */
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return getCurrentSubActivity().onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if(resultCode == Constants.RESULT_GO_HOME) {
        	switchToActivity(SubActivities.TASK_LIST, null);
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
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(getCurrentSubActivity().onOptionsItemSelected(item) == true)
    		return true;
    	else
    		return super.onOptionsItemSelected(item);
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
        if(taskController != null)
        	taskController.close();
        if(tagController != null)
            tagController.close();
        Synchronizer.setTagController(null);
        Synchronizer.setTaskController(null);
    }
}
