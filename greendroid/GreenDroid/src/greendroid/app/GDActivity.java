/*
 * Copyright (C) 2010 Cyril Mottier (http://www.cyrilmottier.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package greendroid.app;

import com.cyrilmottier.android.greendroid.R;

import greendroid.util.Config;
import greendroid.widget.GDActionBar;
import greendroid.widget.ActionBarHost;
import greendroid.widget.ActionBarItem;
import greendroid.widget.GDActionBar.OnActionBarListener;
import greendroid.widget.GDActionBar.Type;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

/**
 * <p>
 * An {@link GDActivity} is a regular Activity that always hosts an
 * {@link GDActionBar}. It is extremely simple to use as you have nothing
 * particular to do. Indeed, the ActionBar is automatically added to your own
 * layout when using the {@link #getContentView()} method. You can also use one
 * of the setActionBarContentView utility methods. As a result, a basic
 * {@link GDActivity} will often be initialized using the following snippet of
 * code:
 * </p>
 * 
 * <pre>
 * protected void onCreate(Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 * 
 *     setActionBarContentView(R.layout.main);
 * }
 * </pre>
 * <p>
 * An ActionBar is a widget that may contains actions items and a title. You can
 * also set the title putting an extra string with the key
 * {@link GD_ACTION_BAR_TITLE} in your Intent:
 * </p>
 * 
 * <pre>
 * Intent intent = new Intent(this, MyGDActivity.class);
 * intent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, &quot;Next screen title&quot;);
 * startActivity(intent);
 * </pre>
 * <p>
 * Note: An {@link GDActivity} automatically handle the type of the ActionBar
 * (Dashboard or Normal) depending on the value returned by the
 * getHomeActivityClass of your {@link GDApplication}. However you can force the
 * type of the action bar in your constructor.
 * </p>
 * 
 * <pre>
 * public MyGDActivity() {
 *     super(ActionBar.Type.Dashboard);
 * }
 * </pre>
 * <p>
 * All Activities that inherits from an {@link GDActivity} are notified when an
 * action button is tapped in the onHandleActionBarItemClick(ActionBarItem, int)
 * method. By default this method does nothing but return false.
 * </p>
 * 
 * @see {@link GDApplication#getHomeActivityClass()}
 * @see {@link ActionBarActivity#GD_ACTION_BAR_TITLE}
 * @see {@link GDActivity#setActionBarContentView(int)}
 * @see {@link GDActivity#setActionBarContentView(View)}
 * @see {@link GDActivity#setActionBarContentView(View, LayoutParams)}
 * @author Cyril Mottier
 */
public class GDActivity extends Activity implements ActionBarActivity {

    private static final String LOG_TAG = GDActivity.class.getSimpleName();

    private boolean mDefaultConstructorUsed = false;

    private Type mActionBarType;
    private ActionBarHost mActionBarHost;

    public GDActivity() {
        this(Type.Normal);
        mDefaultConstructorUsed = true;
    }

    public GDActivity(GDActionBar.Type actionBarType) {
        super();
        mActionBarType = actionBarType;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        ensureLayout();
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mDefaultConstructorUsed) {
            // HACK cyril: This should have been done is the default
            // constructor. Unfortunately, the getApplication() method returns
            // null there. Hence, this has to be done here.
            if (getClass().equals(getGDApplication().getHomeActivityClass())) {
                mActionBarType = Type.Dashboard;
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ensureLayout();
    }

    public GDActionBar.Type getActionBarType() {
        return mActionBarType;
    }

    public int createLayout() {
        switch (mActionBarType) {
            case Dashboard:
                return R.layout.gd_content_dashboard;
            case Empty:
                return R.layout.gd_content_empty;
            case Normal:
            default:
                return R.layout.gd_content_normal;
        }
    }

    protected void ensureLayout() {
        if (!verifyLayout()) {
            setContentView(createLayout());
        }
    }

    protected boolean verifyLayout() {
        return mActionBarHost != null;
    }

    public GDApplication getGDApplication() {
        return (GDApplication) getApplication();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        onPreContentChanged();
        onPostContentChanged();
    }

    public void onPreContentChanged() {
        mActionBarHost = (ActionBarHost) findViewById(R.id.gd_action_bar_host);
        if (mActionBarHost == null) {
            throw new RuntimeException(
                    "Your content must have an ActionBarHost whose id attribute is R.id.gd_action_bar_host");
        }
        mActionBarHost.getActionBar().setOnActionBarListener(mActionBarListener);
    }

    public void onPostContentChanged() {

        boolean titleSet = false;

        final Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra(ActionBarActivity.GD_ACTION_BAR_TITLE);
            if (title != null) {
                titleSet = true;
                setTitle(title);
            }
        }

        if (!titleSet) {
            // No title has been set via the Intent. Let's look in the
            // ActivityInfo
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), 0);
                if (activityInfo.labelRes != 0) {
                    setTitle(activityInfo.labelRes);
                }
            } catch (NameNotFoundException e) {
                // Do nothing
            }
        }

        final int visibility = intent.getIntExtra(ActionBarActivity.GD_ACTION_BAR_VISIBILITY, View.VISIBLE);
        getGDActionBar().setVisibility(visibility);
    }

    @Override
    public void setTitle(CharSequence title) {
        getGDActionBar().setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getString(titleId));
    }

    public GDActionBar getGDActionBar() {
        ensureLayout();
        return mActionBarHost.getActionBar();
    }

    public ActionBarItem addActionBarItem(ActionBarItem item) {
        return getGDActionBar().addItem(item);
    }

    public ActionBarItem addActionBarItem(ActionBarItem item, int itemId) {
        return getGDActionBar().addItem(item, itemId);
    }

    public ActionBarItem addActionBarItem(ActionBarItem.Type actionBarItemType) {
        return getGDActionBar().addItem(actionBarItemType);
    }

    public ActionBarItem addActionBarItem(ActionBarItem.Type actionBarItemType, int itemId) {
        return getGDActionBar().addItem(actionBarItemType, itemId);
    }

    public FrameLayout getContentView() {
        ensureLayout();
        return mActionBarHost.getContentView();
    }

    public void setActionBarContentView(int resID) {
        LayoutInflater.from(this).inflate(resID, getContentView());
    }

    public void setActionBarContentView(View view, LayoutParams params) {
        getContentView().addView(view, params);
    }

    public void setActionBarContentView(View view) {
        getContentView().addView(view);
    }

    public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
        return false;
    }

    private OnActionBarListener mActionBarListener = new OnActionBarListener() {
        public void onActionBarItemClicked(int position) {
            if (position == OnActionBarListener.HOME_ITEM) {

                final GDApplication app = getGDApplication();
                switch (mActionBarType) {
                    case Normal:
                        final Class<?> klass = app.getHomeActivityClass();
                        if (klass != null && !klass.equals(GDActivity.this.getClass())) {
                            if (Config.GD_INFO_LOGS_ENABLED) {
                                Log.i(LOG_TAG, "Going back to the home activity");
                            }
                            Intent homeIntent = new Intent(GDActivity.this, klass);
                            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(homeIntent);
                        }
                        break;
                    case Dashboard:
                        final Intent appIntent = app.getMainApplicationIntent();
                        if (appIntent != null) {
                            if (Config.GD_INFO_LOGS_ENABLED) {
                                Log.i(LOG_TAG, "Launching the main application Intent");
                            }
                            startActivity(appIntent);
                        }
                        break;
                }

            } else {
                if (!onHandleActionBarItemClick(getGDActionBar().getItem(position), position)) {
                    if (Config.GD_WARNING_LOGS_ENABLED) {
                        Log.w(LOG_TAG, "Click on item at position " + position + " dropped down to the floor");
                    }
                }
            }
        }
    };

}
