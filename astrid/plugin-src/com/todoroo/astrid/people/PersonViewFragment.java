package com.todoroo.astrid.people;

import android.content.Intent;
import android.support.v4.view.Menu;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.helper.ProgressBarSyncResultCallback;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.ThemeService;

public class PersonViewFragment extends TaskListFragment {

    public static final String EXTRA_USER_ID_LOCAL = "user_local_id"; //$NON-NLS-1$

    public static final String EXTRA_HIDE_QUICK_ADD = "hide_quickAdd"; //$NON-NLS-1$

    private static final String LAST_FETCH_KEY = "actfm_last_user_"; //$NON-NLS-1$

    protected static final int MENU_REFRESH_ID = MENU_SUPPORT_ID + 1;

    @Autowired UserDao userDao;

    @Autowired SyncV2Service syncService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

    protected View taskListView;

    private User user;

    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.task_list_body_person, root, false);

        taskListView = super.getListBody(parent);
        parent.addView(taskListView);

        return parent;
    }

    @Override
    protected void initializeData() {
        super.initializeData();
        if (extras.containsKey(EXTRA_USER_ID_LOCAL)) {
            user = userDao.fetch(extras.getLong(EXTRA_USER_ID_LOCAL), User.PROPERTIES);
        }
        ((TextView)taskListView.findViewById(android.R.id.empty)).setText(getEmptyDisplayString());
    }

    @Override
    protected void setupQuickAddBar() {
        super.setupQuickAddBar();
        quickAddBar.setUsePeopleControl(false);
        if (user != null)
            quickAddBar.getQuickAddBox().setHint(getString(R.string.TLA_quick_add_hint_assign, user.getDisplayName()));

        if (extras.containsKey(EXTRA_HIDE_QUICK_ADD))
            quickAddBar.setVisibility(View.GONE);

    }

    @Override
    protected void addSyncRefreshMenuItem(Menu menu, int themeFlags) {
        if(actFmPreferenceService.isLoggedIn()) {
            addMenuItem(menu, R.string.actfm_TVA_menu_refresh,
                    ThemeService.getDrawable(R.drawable.icn_menu_refresh, themeFlags), MENU_REFRESH_ID, true);
        } else {
            super.addSyncRefreshMenuItem(menu, themeFlags);
        }
    }

    @Override
    public boolean handleOptionsMenuItemSelected(int id, Intent intent) {
        switch (id) {
        case MENU_REFRESH_ID:
            refreshData(true);
            return true;
        }
        return super.handleOptionsMenuItemSelected(id, intent);
    }

    @Override
    protected void initiateAutomaticSyncImpl() {
        if (!isCurrentTaskListFragment())
            return;
        if (user != null) {
            long lastAutoSync = Preferences.getLong(LAST_FETCH_KEY + user.getId(), 0);
            if (DateUtilities.now() - lastAutoSync > DateUtilities.ONE_HOUR)
                refreshData(false);
        }
    }

    private void refreshData(final boolean manual) {
        if (user != null) {
            ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.DLG_loading);

            syncService.synchronizeList(user, manual, new ProgressBarSyncResultCallback(getActivity(), this,
                    R.id.progressBar, new Runnable() {
                @Override
                public void run() {
                    if (manual)
                        ContextManager.getContext().sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                    else
                        refresh();
                    ((TextView)taskListView.findViewById(android.R.id.empty)).setText(getEmptyDisplayString());
                }
            }));
        }
    }

    private String getEmptyDisplayString() {
        String userName = user != null ? user.getDisplayName() : null;
        return TextUtils.isEmpty(userName) ? getString(R.string.actfm_my_shared_tasks_empty) : getString(R.string.TLA_no_items_person, userName);
    }

}
