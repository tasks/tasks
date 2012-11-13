/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.content.res.Resources;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.service.ThemeService;

public class ReengagementFragment extends TaskListFragment {

    public static final String EXTRA_TEXT = "dialogText"; //$NON-NLS-1$

    @Override
    protected void initializeData() {
        // hide quick add
        getView().findViewById(R.id.taskListFooter).setVisibility(View.GONE);

        super.initializeData();

        setupSpeechBubble();
    }

    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.task_list_body_reengagement, root, false);

        View taskListView = super.getListBody(parent);
        parent.addView(taskListView, 0);

        return parent;
    }

    @Override
    protected void refresh() {
        super.refresh();
        setupSpeechBubble();
    }

    private void setupSpeechBubble() {
        Resources r = getActivity().getResources();
        TextView snooze = (TextView) getView().findViewById(R.id.reminder_snooze);
        snooze.setBackgroundColor(r.getColor(ThemeService.getThemeColor()));
        TextView reminder = (TextView) getView().findViewById(R.id.reminder_message);
        if (taskAdapter.getCount() == 0) {
            snooze.setText(R.string.rmd_reengage_add_tasks);
            snooze.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    quickAddBar.performButtonClick();
                }
            });
        } else {
            snooze.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            });
        }

        reminder.setText(extras.getString(EXTRA_TEXT));
    }

}
