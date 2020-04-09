package org.tasks.injection;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import dagger.Module;
import dagger.Provides;

@Module
class DialogFragmentModule {

  private final DialogFragment dialogFragment;

  DialogFragmentModule(DialogFragment dialogFragment) {
    this.dialogFragment = dialogFragment;
  }

  @Provides
  public Fragment getFragment() {
    return dialogFragment;
  }
}
