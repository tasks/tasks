package org.tasks.injection;

import android.app.ListActivity;
import android.os.Bundle;

import org.tasks.analytics.Tracker;

import javax.inject.Inject;

import dagger.ObjectGraph;

public abstract class InjectingListActivity extends ListActivity implements Injector {
    private ObjectGraph objectGraph;

    @Inject Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        objectGraph = ((Injector) getApplication()).getObjectGraph().plus(new ActivityModule(this));
        inject(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void inject(Object caller) {
        objectGraph.inject(caller);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }

    @Override
    protected void onResume() {
        super.onResume();

        tracker.showScreen(getClass().getSimpleName());
    }
}
