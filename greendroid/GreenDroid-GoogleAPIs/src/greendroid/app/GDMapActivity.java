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

import greendroid.app.ActionBarActivity;
import greendroid.app.GDApplication;
import greendroid.util.Config;
import greendroid.widget.ActionBar;
import greendroid.widget.ActionBar.OnActionBarListener;
import greendroid.widget.ActionBar.Type;
import greendroid.widget.ActionBarHost;
import greendroid.widget.ActionBarItem;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.cyrilmottier.android.greendroid.R;
import com.google.android.maps.MapActivity;

/**
 * A special {@link MapActivity} that manages an ActionBar. See
 * {@link GDActivity} for a complete documentation of all available features.
 * 
 * @see GDActivity
 * @author Cyril Mottier
 */
public abstract class GDMapActivity extends MapActivity implements ActionBarActivity {

    private static final String LOG_TAG = GDMapActivity.class.getSimpleName();

    private boolean mDefaultConstructorUsed = false;

    private Type mActionBarType;
    private ActionBarHost mActionBarHost;

    public GDMapActivity() {
        this(Type.Normal);
        mDefaultConstructorUsed = true;
    }

    public GDMapActivity(ActionBar.Type actionBarType) {
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

    public ActionBar.Type getActionBarType() {
        return mActionBarType;
    }

    public int createLayout() {
        switch (mActionBarType) {
            case Dashboard:
                return R.layout.gd_content_dashboard;
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
        getActionBar().setVisibility(visibility);
    }

    @Override
    public void setTitle(CharSequence title) {
        getActionBar().setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getString(titleId));
    }

    public ActionBar getActionBar() {
        ensureLayout();
        return mActionBarHost.getActionBar();
    }

    public ActionBarItem addActionBarItem(ActionBarItem item) {
        return getActionBar().addItem(item);
    }

    public ActionBarItem addActionBarItem(ActionBarItem item, int itemId) {
        return getActionBar().addItem(item, itemId);
    }

    public ActionBarItem addActionBarItem(ActionBarItem.Type actionBarItemType) {
        return getActionBar().addItem(actionBarItemType);
    }

    public ActionBarItem addActionBarItem(ActionBarItem.Type actionBarItemType, int itemId) {
        return getActionBar().addItem(actionBarItemType, itemId);
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
                        if (klass != null && !klass.equals(GDMapActivity.this.getClass())) {
                            if (Config.GD_INFO_LOGS_ENABLED) {
                                Log.i(LOG_TAG, "Going back to the home activity");
                            }
                            Intent homeIntent = new Intent(GDMapActivity.this, klass);
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
                if (!onHandleActionBarItemClick(getActionBar().getItem(position), position)) {
                    if (Config.GD_WARNING_LOGS_ENABLED) {
                        Log.w(LOG_TAG, "Click on item at position " + position + " dropped down to the floor");
                    }
                }
            }
        }
    };

}
