package org.tasks.injection;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import dagger.ObjectGraph;

public abstract class InjectingPreferenceActivity extends PreferenceActivity implements Injector {
    private ObjectGraph objectGraph;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
}
