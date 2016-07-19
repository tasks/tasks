package org.tasks.injection;

import android.app.Activity;
import android.app.DialogFragment;

public abstract class InjectingNativeDialogFragment extends DialogFragment {
    private boolean injected;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!injected) {
            inject(((InjectingActivity) activity)
                    .getComponent()
                    .plus(new NativeDialogFragmentModule(this)));
            injected = true;
        }
    }

    protected abstract void inject(NativeDialogFragmentComponent component);
}
