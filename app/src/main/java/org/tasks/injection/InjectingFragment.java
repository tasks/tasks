package org.tasks.injection;

import android.app.Activity;
import android.support.v4.app.Fragment;

public abstract class InjectingFragment extends Fragment {

  private boolean injected;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    if (!injected) {
      inject(((InjectingActivity) activity).getComponent().plus(new FragmentModule(this)));
      injected = true;
    }
  }

  protected abstract void inject(FragmentComponent component);
}
