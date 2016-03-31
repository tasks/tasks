package org.tasks.injection;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.preferences.AppCompatPreferenceActivity;
import org.tasks.preferences.ThemeApplicator;

import javax.inject.Inject;

public abstract class InjectingPreferenceActivity extends AppCompatPreferenceActivity implements InjectingActivity {

    private ActivityComponent activityComponent;

    protected Toolbar toolbar;

    @Inject ThemeApplicator themeApplicator;
    @Inject Tracker tracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        activityComponent = ((InjectingApplication) getApplication())
                .getComponent()
                .plus(new ActivityModule(this));
        inject(activityComponent);

        themeApplicator.applyThemeAndStatusBarColor();

        super.onCreate(savedInstanceState);

        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        View content = root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.activity_prefs, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);

        toolbar = (Toolbar) toolbarContainer.findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_arrow_back_24dp));
        DrawableCompat.setTint(drawable, getResources().getColor(android.R.color.white));
        toolbar.setNavigationIcon(drawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public ActivityComponent getComponent() {
        return activityComponent;
    }

    @Override
    protected void onResume() {
        super.onResume();

        tracker.showScreen(getClass().getSimpleName());
    }

    protected void requires(int prefGroup, boolean passesCheck, int... resIds) {
        if (!passesCheck) {
            remove((PreferenceCategory) findPreference(getString(prefGroup)), resIds);
        }
    }

    protected void requires(boolean passesCheck, int... resIds) {
        if (!passesCheck) {
            remove(getPreferenceScreen(), resIds);
        }
    }

    private void remove(PreferenceGroup preferenceGroup, int[] resIds) {
        for (int resId : resIds) {
            preferenceGroup.removePreference(getPref(resId));
        }
    }

    protected Preference getPref(int resId) {
        return findPreference(getString(resId));
    }
}
