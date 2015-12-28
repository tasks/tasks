package org.tasks.injection;

import android.app.Activity;
import android.app.Fragment;

import dagger.ObjectGraph;

public class InjectingFragment extends Fragment implements Injector {

    private boolean injected;
    private ObjectGraph objectGraph;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!injected) {
            objectGraph = ((Injector) activity).getObjectGraph().plus(new FragmentModule(this, this));
            inject(this);
            injected = true;
        }
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
