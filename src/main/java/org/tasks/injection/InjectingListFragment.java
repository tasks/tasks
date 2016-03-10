package org.tasks.injection;

import android.app.Activity;
import android.app.ListFragment;

public abstract class InjectingListFragment extends ListFragment {

    private boolean injected;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!injected) {
            inject(((InjectingActivity) activity)
                    .getComponent()
                    .plus(new FragmentModule(this)));
            injected = true;
        }
    }

    public abstract void inject(FragmentComponent component);
}
