package org.tasks.injection;

import android.content.Context;
import android.support.v4.app.Fragment;

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
