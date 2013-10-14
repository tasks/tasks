package com.todoroo.astrid.activity;

import android.content.Context;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.CoreFilterExposer;

public class DefaultFilterMode implements FilterModeSpec {

    @Override
    public Class<? extends FilterListFragment> getFilterListClass() {
        return FilterListFragment.class;
    }

    @Override
    public Filter getDefaultFilter(Context context) {
        return CoreFilterExposer.buildInboxFilter(context.getResources());
    }
}
