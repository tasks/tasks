package com.todoroo.astrid.people;

import android.content.Context;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.activity.FilterModeSpec;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.ui.MainMenuPopover;

public class PeopleFilterMode implements FilterModeSpec {

    @Override
    public Filter getDefaultFilter(Context context) {
        Filter defaultFilter = PeopleFilterExposer.mySharedTasks(context);
        return defaultFilter;
    }

    @Override
    public Class<? extends FilterListFragment> getFilterListClass() {
        return PeopleListFragment.class;
    }

    @Override
    public void onFilterItemClickedCallback(FilterListItem item) {/**/}

    @Override
    public int[] getForbiddenMenuItems() {
        return FORBIDDEN_MENU_ITEMS;
    }

    @Override
    public int getMainMenuIconAttr() {
        return R.attr.asPeopleMenu;
    }

    private static final int[] FORBIDDEN_MENU_ITEMS = {
        TaskListFragment.MENU_NEW_FILTER_ID,
        MainMenuPopover.MAIN_MENU_ITEM_FRIENDS
    };

    @Override
    public boolean showComments() {
        return false;
    }
}
