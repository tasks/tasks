package org.tasks.injection;

import android.app.ListActivity;
import android.os.Bundle;

import org.tasks.analytics.Tracker;

import javax.inject.Inject;

public abstract class InjectingListActivity extends ListActivity implements InjectingActivity {

    private ActivityComponent activityComponent;

    @Inject Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activityComponent = ((InjectingApplication) getApplication())
                .getComponent()
                .plus(new ActivityModule(this));
        inject(activityComponent);

        super.onCreate(savedInstanceState);
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
}
