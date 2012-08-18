package com.todoroo.astrid.activity;

import android.content.Context;

import com.timsu.astrid.R;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.CoreFilterExposer;

public class DefaultFilterMode implements FilterModeSpec {

    @Override
    public int[] getForbiddenMenuItems() {
        return new int[0];
    }

    @Override
    public Class<? extends FilterListFragment> getFilterListClass() {
        return FilterListFragment.class;
    }

    @Override
    public Filter getDefaultFilter(Context context) {
        return CoreFilterExposer.buildInboxFilter(context.getResources());
    }

    @Override
    public int getMainMenuIconAttr() {
        return R.attr.asMainMenu;
    }

    @Override
    public void onFilterItemClickedCallback(FilterListItem item) {
        //
    }

}
