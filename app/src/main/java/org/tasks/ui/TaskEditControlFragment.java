package org.tasks.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.injection.InjectingFragment;

import butterknife.ButterKnife;

public abstract class TaskEditControlFragment extends InjectingFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.control_set_template, null);
        LinearLayout content = (LinearLayout) view.findViewById(R.id.content);
        inflater.inflate(getLayout(), content);
        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        icon.setImageResource(getIcon());
        ButterKnife.bind(this, view);
        return view;
    }

    protected abstract int getLayout();

    protected abstract int getIcon();

    public abstract int controlId();

    public abstract void initialize(boolean isNewTask, Task task);

    public abstract void apply(Task task);

    public boolean hasChanges(Task original) {
        return false;
    }
}
