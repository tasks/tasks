package org.tasks.injection;

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
  public Fragment getFragment() {
    return fragment;
  }
}
