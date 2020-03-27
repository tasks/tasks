package org.tasks.injection;

import android.app.Activity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public abstract class InjectingBottomSheetDialogFragment extends BottomSheetDialogFragment {

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
