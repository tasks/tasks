package org.tasks.injection;

import android.app.Activity;
import android.support.v4.app.DialogFragment;

public abstract class InjectingDialogFragment extends DialogFragment {

  private boolean injected;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    if (!injected) {
      inject(((InjectingActivity) activity).getComponent().plus(new DialogFragmentModule(this)));
      injected = true;
    }
  }

  protected abstract void inject(DialogFragmentComponent component);
}
