package org.tasks.injection;

import android.app.Activity;
import android.support.v4.app.ListFragment;

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

    protected abstract void inject(FragmentComponent component);
}
