package org.tasks.injection;

import android.app.Activity;
import android.support.v4.app.ListFragment;

import dagger.ObjectGraph;

public class InjectingListFragment extends ListFragment implements Injector {

    private boolean injected;
    private ObjectGraph objectGraph;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!injected) {
            objectGraph = ((Injector) activity).getObjectGraph().plus(new FragmentModule(activity, this));
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
