package org.tasks.injection;

import android.os.Bundle;

import com.todoroo.astrid.utility.TodorooPreferenceActivity;

import dagger.ObjectGraph;

public abstract class InjectingSyncProviderPreferences extends TodorooPreferenceActivity implements Injector {
    private ObjectGraph objectGraph;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        objectGraph = ((Injector) getApplication()).getObjectGraph().plus(new ActivityModule(this, this));
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
