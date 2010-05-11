package com.todoroo.astrid.test;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.test.ActivityInstrumentationTestCase2;
import android.view.MotionEvent;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.TestDependencyInjector;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.test.DatabaseTestCase.AstridTestDatabase;

/**
 * ActivityTestCase is a helper for testing Todoroo Activities.
 * <p>
 * It initializes a test database before the activity is created.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 * @param <T>
 */
public class AstridActivityTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> {

	@Autowired
	public Database database;

    static {
        AstridDependencyInjector.initialize();
        TestDependencyInjector.initialize("db").addInjectable("database",
                new AstridTestDatabase());
    }

	public AstridActivityTestCase(String packageName, Class<T> activityClass) {
        super(packageName, activityClass);
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
	protected void setUp() throws Exception {
        super.setUp();

		// create new test database
        AstridTestDatabase.dropTables(getInstrumentation().getTargetContext());
        database.openForWriting();
	}

	@Override
	protected void tearDown() throws Exception {
	    super.tearDown();

	    if(database != null)
	        database.close();
	}

	/**
	 * Call to just tear this activity down
	 */
	protected void tearActivityDown() throws Exception {
	    super.tearDown();
	}

	/**
	 * Call to just set this activity up
	 */
	protected void setActivityUp() throws Exception {
	    super.setUp();
	}

    /** Calls various lifecycle methods, makes sure all of them work */
    public void testLifecycle() throws Exception {
        T activity = getActivity();
        getInstrumentation().callActivityOnPause(activity);
        Thread.sleep(500);
        getInstrumentation().callActivityOnResume(activity);

        getInstrumentation().sendPointerSync(MotionEvent.obtain(500, 500, MotionEvent.ACTION_DOWN, 10, 30, 0));

        getInstrumentation().callActivityOnPause(activity);
        getInstrumentation().callActivityOnStop(activity);
        Thread.sleep(500);
        getInstrumentation().callActivityOnRestart(activity);
        getInstrumentation().callActivityOnStart(activity);
        getInstrumentation().callActivityOnResume(activity);

        getInstrumentation().sendPointerSync(MotionEvent.obtain(500, 500, MotionEvent.ACTION_DOWN, 10, 30, 0));

        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        Thread.sleep(500);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Thread.sleep(1000);
    }


}
