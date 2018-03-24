package org.tasks.injection;

import android.app.Activity;
import android.app.DialogFragment;
import dagger.Module;
import dagger.Provides;

@Module
public class NativeDialogFragmentModule {

  private final DialogFragment dialogFragment;

  public NativeDialogFragmentModule(DialogFragment dialogFragment) {
    this.dialogFragment = dialogFragment;
  }

  @Provides
  public Activity getActivity() {
    return dialogFragment.getActivity();
  }
}
