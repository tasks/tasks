package org.tasks.injection;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.tasks.analytics.Tracker;

import javax.inject.Inject;

import dagger.ObjectGraph;

public abstract class InjectingAppCompatActivity extends AppCompatActivity implements Injector {
    private ObjectGraph objectGraph;

    @Inject Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        objectGraph = ((Injector) getApplication()).getObjectGraph().plus(new ActivityModule(this));
        inject(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        tracker.showScreen(getClass().getSimpleName());
    }

    @Override
    public void inject(Object caller) {
        objectGraph.inject(caller);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }
}
