package org.tasks.injection;

import android.app.Fragment;
import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class FragmentModule {
    private final Fragment fragment;

    public FragmentModule(Fragment fragment) {
        this.fragment = fragment;
    }

    @Provides
    @ForActivity
    public Context getContext() {
        return fragment.getActivity();
    }

    @Provides
    public Fragment getFragment() {
        return fragment;
    }
}
