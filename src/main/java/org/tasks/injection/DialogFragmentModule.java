package org.tasks.injection;

import android.app.Activity;
import android.app.DialogFragment;

import dagger.Module;
import dagger.Provides;

@Module
public class DialogFragmentModule {
    private DialogFragment dialogFragment;

    public DialogFragmentModule(DialogFragment dialogFragment) {
        this.dialogFragment = dialogFragment;
    }

    @Provides
    public Activity getActivity() {
        return dialogFragment.getActivity();
    }
}
