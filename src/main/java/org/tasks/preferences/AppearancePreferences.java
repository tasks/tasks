package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.api.Filter;

import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;

import javax.inject.Inject;

public class AppearancePreferences extends InjectingPreferenceActivity {

    private static final int REQUEST_CUSTOMIZE = 1004;
    private static final int REQUEST_DEFAULT_LIST = 1005;

    private static final String EXTRA_BUNDLE = "extra_bundle";
    public static String EXTRA_RESTART = "extra_restart";
    public static String EXTRA_FILTERS_CHANGED = "extra_filters_changed";

    @Inject Preferences preferences;
    @Inject DefaultFilterProvider defaultFilterProvider;

    private Bundle result;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        result = savedInstanceState == null
                ? new Bundle()
                : savedInstanceState.getBundle(EXTRA_BUNDLE);

        addPreferencesFromResource(R.xml.preferences_appearance);

        setExtraOnChange(R.string.p_fontSize, EXTRA_RESTART);
        setExtraOnChange(R.string.p_fullTaskTitle, EXTRA_RESTART);
        setExtraOnChange(R.string.p_show_today_filter, EXTRA_FILTERS_CHANGED);
        setExtraOnChange(R.string.p_show_recently_modified_filter, EXTRA_FILTERS_CHANGED);
        setExtraOnChange(R.string.p_show_not_in_list_filter, EXTRA_FILTERS_CHANGED);
        findPreference(getString(R.string.customize_edit_screen)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(AppearancePreferences.this, BeastModePreferences.class), REQUEST_CUSTOMIZE);
                return true;
            }
        });
        Preference defaultList = findPreference(getString(R.string.p_default_list));
        Filter filter = defaultFilterProvider.getDefaultFilter();
        defaultList.setSummary(filter.listingTitle);
        defaultList.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(AppearancePreferences.this, FilterSelectionActivity.class) {{
                    putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
                }}, REQUEST_DEFAULT_LIST);
                return true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBundle(EXTRA_BUNDLE, result);
    }

    @Override
    public void finish() {
        setResult(RESULT_OK, new Intent() {{
            putExtras(result);
        }});
        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CUSTOMIZE) {
            if (resultCode == RESULT_OK) {
                result.putBoolean(EXTRA_RESTART, true);
            }
        } else if (requestCode == REQUEST_DEFAULT_LIST) {
            if (resultCode == RESULT_OK) {
                Filter filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
                defaultFilterProvider.setDefaultFilter(filter);
                findPreference(getString(R.string.p_default_list)).setSummary(filter.listingTitle);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setExtraOnChange(int resId, final String extra) {
        findPreference(getString(resId)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                result.putBoolean(extra, true);
                return true;
            }
        });
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }
}
