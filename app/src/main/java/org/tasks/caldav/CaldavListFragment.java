package org.tasks.caldav;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.CaldavFilter;

import org.tasks.R;
import org.tasks.data.CaldavAccount;
import org.tasks.injection.FragmentComponent;

public class CaldavListFragment extends TaskListFragment {

    public static TaskListFragment newCaldavListFragment(CaldavFilter filter, CaldavAccount account) {
        CaldavListFragment fragment = new CaldavListFragment();
        fragment.filter = filter;
        fragment.account = account;
        return fragment;
    }

    private static final String EXTRA_CALDAV_ACCOUNT = "extra_caldav_account";

    protected CaldavAccount account;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.account = savedInstanceState.getParcelable(EXTRA_CALDAV_ACCOUNT);
        }
    }

    @Override
    protected void inflateMenu(Toolbar toolbar) {
        super.inflateMenu(toolbar);
        toolbar.inflateMenu(R.menu.menu_caldav_list_fragment);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_CALDAV_ACCOUNT, account);
    }

    @Override
    protected boolean hasDraggableOption() {
        return false;
    }

    @Override
    public void inject(FragmentComponent component) {
        component.inject(this);
    }
}
