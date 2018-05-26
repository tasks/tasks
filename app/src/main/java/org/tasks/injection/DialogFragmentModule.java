package org.tasks.injection;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import dagger.Module;
import dagger.Provides;

@Module
public class DialogFragmentModule {

  private final DialogFragment dialogFragment;

  public DialogFragmentModule(DialogFragment dialogFragment) {
    this.dialogFragment = dialogFragment;
  }

  @Provides
  public Fragment getFragment() {
    return dialogFragment;
  }
}
