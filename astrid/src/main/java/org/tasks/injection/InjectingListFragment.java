package org.tasks.injection;

import android.app.Activity;
import android.support.v4.app.ListFragment;

public class InjectingListFragment extends ListFragment {

    private boolean injected;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!injected) {
            ((Injector) activity.getApplication()).inject(this, new FragmentModule());
            injected = true;
        }
    }
}
