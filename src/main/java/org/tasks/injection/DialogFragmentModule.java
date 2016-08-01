package org.tasks.injection;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

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

    @Provides
    public Fragment getFragment() {
        return dialogFragment;
    }

    @Provides
    @ForActivity
    public Context getContext() {
        return dialogFragment.getActivity();
    }
}
