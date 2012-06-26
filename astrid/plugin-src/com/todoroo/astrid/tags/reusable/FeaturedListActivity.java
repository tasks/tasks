package com.todoroo.astrid.tags.reusable;

import android.content.Intent;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.FilterListItem;

public class FeaturedListActivity extends AstridActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.featured_list_activity);
    }

    @Override
    public boolean onFilterItemClicked(FilterListItem item) {
        Intent taskList = new Intent(this, TaskListActivity.class);
        taskList.putExtra(TaskListFragment.TOKEN_FILTER, item);
        startActivity(taskList);
        return true;
    }

}
