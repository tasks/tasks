package org.tasks.injection;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;

public abstract class InjectingNativeDialogFragment extends DialogFragment {

  private boolean injected;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    if (!injected) {
      inject(
          ((InjectingActivity) activity).getComponent().plus(new NativeDialogFragmentModule(this)));
      injected = true;
    }
  }

  @Override
  public void onDestroyView() {
    // https://code.google.com/p/android/issues/detail?id=17423
    Dialog dialog = getDialog();
    if (dialog != null && getRetainInstance()) {
      dialog.setDismissMessage(null);
    }
    super.onDestroyView();
  }

  protected abstract void inject(NativeDialogFragmentComponent component);
}
