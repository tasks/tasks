package com.todoroo.astrid.people;

import android.app.Activity;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.adapter.FilterAdapter;

public class PeopleListFragment extends FilterListFragment {

    @Override
    protected FilterAdapter instantiateAdapter() {
        return new PeopleFilterAdapter(getActivity(), null, R.layout.filter_adapter_row, false);
    }

    @Override
    protected int getLayout(Activity activity) {
        if (AndroidUtilities.isTabletSized(activity))
            return R.layout.people_list_fragment_3pane;
        else
            return R.layout.people_list_fragment;
    }
}
