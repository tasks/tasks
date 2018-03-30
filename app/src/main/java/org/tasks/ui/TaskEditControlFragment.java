package org.tasks.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import butterknife.ButterKnife;
import com.todoroo.astrid.data.Task;
import org.tasks.R;
import org.tasks.injection.InjectingFragment;

public abstract class TaskEditControlFragment extends InjectingFragment {

  public static final String EXTRA_TASK = "extra_task";

  protected Task task;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.control_set_template, null);
    LinearLayout content = view.findViewById(R.id.content);
    inflater.inflate(getLayout(), content);
    ImageView icon = view.findViewById(R.id.icon);
    icon.setImageResource(getIcon());
    ButterKnife.bind(this, view);
    return view;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    Bundle arguments = getArguments();
    if (arguments != null) {
      task = arguments.getParcelable(EXTRA_TASK);
    }
  }

  protected abstract int getLayout();

  protected abstract int getIcon();

  public abstract int controlId();

  public abstract void apply(Task task);

  public boolean hasChanges(Task original) {
    return false;
  }
}
