package com.todoroo.astrid.reminders;

import android.content.res.Resources;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.DisposableTaskListFragment;
import com.todoroo.astrid.service.ThemeService;

public class ReengagementFragment extends DisposableTaskListFragment {

    @Override
    protected void initializeData() {
        // hide quick add
        getView().findViewById(R.id.taskListFooter).setVisibility(View.GONE);

        Resources r = getActivity().getResources();

        int color = r.getColor(ThemeService.getThemeColor());

        View snooze = getView().findViewById(R.id.reminder_snooze);
        snooze.setBackgroundColor(color);
        snooze.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        TextView reminder = (TextView) getView().findViewById(R.id.reminder_message);
        reminder.setText(Notifications.getRandomReminder(r.getStringArray(R.array.rmd_reengage_dialog_options)));

        super.initializeData();
    }

    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.task_list_body_reengagement, root, false);

        View taskListView = super.getListBody(parent);
        parent.addView(taskListView, 0);

        return parent;
    }

}
