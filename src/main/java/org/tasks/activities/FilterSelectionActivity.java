package org.tasks.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;

import org.tasks.dialogs.DialogBuilder;
import org.tasks.filters.FilterCounter;
import org.tasks.filters.FilterProvider;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

public class FilterSelectionActivity extends InjectingAppCompatActivity {

    public static final String EXTRA_FILTER_NAME = "extra_filter_name";
    public static final String EXTRA_FILTER_SQL = "extra_filter_query";
    public static final String EXTRA_FILTER_VALUES = "extra_filter_values";

    @Inject FilterProvider filterProvider;
    @Inject FilterCounter filterCounter;
    @Inject DialogBuilder dialogBuilder;
    @Inject ActivityPreferences activityPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityPreferences.applyTheme();

        final FilterAdapter filterAdapter = new FilterAdapter(filterProvider, filterCounter, this, null, false);
        filterAdapter.populateList();

        dialogBuilder.newDialog()
                .setSingleChoiceItems(filterAdapter, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Filter selectedFilter = (Filter) filterAdapter.getItem(which);
                        setResult(RESULT_OK, new Intent() {{
                            putExtra(EXTRA_FILTER_NAME, selectedFilter.listingTitle);
                            putExtra(EXTRA_FILTER_SQL, selectedFilter.getSqlQuery());
                            if (selectedFilter.valuesForNewTasks != null) {
                                putExtra(EXTRA_FILTER_VALUES, AndroidUtilities.contentValuesToSerializedString(selectedFilter.valuesForNewTasks));
                            }
                        }});
                        dialog.dismiss();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();
    }
}
