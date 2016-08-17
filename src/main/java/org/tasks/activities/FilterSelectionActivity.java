package org.tasks.activities;

import android.content.Intent;
import android.os.Bundle;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;

import org.tasks.dialogs.DialogBuilder;
import org.tasks.filters.FilterCounter;
import org.tasks.filters.FilterProvider;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.locale.Locale;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;

import javax.inject.Inject;

public class FilterSelectionActivity extends InjectingAppCompatActivity {

    public static final String EXTRA_RETURN_FILTER = "extra_include_filter";
    public static final String EXTRA_FILTER = "extra_filter";
    private static final String EXTRA_FILTER_NAME = "extra_filter_name";
    private static final String EXTRA_FILTER_SQL = "extra_filter_query";
    private static final String EXTRA_FILTER_VALUES = "extra_filter_values";

    @Inject FilterProvider filterProvider;
    @Inject FilterCounter filterCounter;
    @Inject DialogBuilder dialogBuilder;
    @Inject Theme theme;
    @Inject ThemeCache themeCache;
    @Inject Locale locale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final boolean returnFilter = getIntent().getBooleanExtra(EXTRA_RETURN_FILTER, false);

        final FilterAdapter filterAdapter = new FilterAdapter(filterProvider, filterCounter, this,
                false, theme, themeCache, locale);
        filterAdapter.populateList();

        dialogBuilder.newDialog()
                .setSingleChoiceItems(filterAdapter, -1, (dialog, which) -> {
                    final Filter selectedFilter = (Filter) filterAdapter.getItem(which);
                    setResult(RESULT_OK, new Intent() {{
                        if (returnFilter) {
                            putExtra(EXTRA_FILTER, selectedFilter);
                        }
                        putExtra(EXTRA_FILTER_NAME, selectedFilter.listingTitle);
                        putExtra(EXTRA_FILTER_SQL, selectedFilter.getSqlQuery());
                        if (selectedFilter.valuesForNewTasks != null) {
                            putExtra(EXTRA_FILTER_VALUES, AndroidUtilities.contentValuesToSerializedString(selectedFilter.valuesForNewTasks));
                        }
                    }});
                    dialog.dismiss();
                })
                .setOnDismissListener(dialog -> finish())
                .show();
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }
}
