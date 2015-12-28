package org.tasks.injection;

import android.app.Activity;
import android.app.DialogFragment;

public class InjectingDialogFragment extends DialogFragment {
    private boolean injected;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!injected) {
            ((Injector) activity).getObjectGraph().plus(new DialogFragmentModule(this)).inject(this);
            injected = true;
        }
    }
}
