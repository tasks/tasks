package org.tasks.locale.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.injection.ActivityComponent;
import org.tasks.locale.bundle.PluginBundleValues;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.ui.MenuColorizer;

import java.util.Set;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public final class TaskerSettingsActivity extends AbstractFragmentPluginAppCompatActivity {

    private static final int REQUEST_SELECT_FILTER = 10124;

    @Bind(R.id.toolbar) Toolbar toolbar;

    @Inject ActivityPreferences preferences;

    private Bundle previousBundle;
    private String title;
    private String query;
    private String values;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences.applyThemeAndStatusBarColor();
        setContentView(R.layout.tasker_settings);
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            previousBundle = savedInstanceState.getParcelable(PluginBundleValues.BUNDLE_EXTRA_PREVIOUS_BUNDLE);
            title = savedInstanceState.getString(PluginBundleValues.BUNDLE_EXTRA_STRING_TITLE);
            query = savedInstanceState.getString(PluginBundleValues.BUNDLE_EXTRA_STRING_QUERY);
            values = savedInstanceState.getString(PluginBundleValues.BUNDLE_EXTRA_STRING_VALUES);
            updateActivity();
        }

        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_close_24dp));
            DrawableCompat.setTint(drawable, getResources().getColor(android.R.color.white));
            supportActionBar.setHomeAsUpIndicator(drawable);
            supportActionBar.setDisplayShowTitleEnabled(false);
        }
    }

    @OnClick(R.id.filter_selection)
    void selectFilter() {
        startActivityForResult(new Intent(TaskerSettingsActivity.this, FilterSelectionActivity.class), REQUEST_SELECT_FILTER);
    }

    @Override
    public void onPostCreateWithPreviousResult(final Bundle previousBundle, final String previousBlurb) {
        this.previousBundle = previousBundle;
        title = PluginBundleValues.getTitle(previousBundle);
        query = PluginBundleValues.getQuery(previousBundle);
        updateActivity();
    }

    @Override
    public boolean isBundleValid(final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Override
    public Bundle getResultBundle() {
        return PluginBundleValues.generateBundle(title, query, values);
    }

    @Override
    public String getResultBlurb(final Bundle bundle) {
        return PluginBundleValues.getTitle(bundle);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.tasker_menu, menu);
        MenuColorizer.colorMenu(this, menu, getResources().getColor(android.R.color.white));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                finish();
                break;
            case android.R.id.home:
                if (equalBundles(getResultBundle(), previousBundle)) {
                    cancel();
                } else {
                    new AlertDialog.Builder(this, R.style.TasksDialog)
                            .setMessage(R.string.discard_changes)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cancel();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .show();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void cancel() {
        mIsCancelled = true;
        finish();
    }

    private boolean equalBundles(Bundle one, Bundle two) {
        if (one == null) {
            return two == null;
        }
        if (two == null) {
            return false;
        }

        if(one.size() != two.size())
            return false;

        Set<String> setOne = one.keySet();
        Object valueOne;
        Object valueTwo;

        for(String key : setOne) {
            valueOne = one.get(key);
            valueTwo = two.get(key);
            if(valueOne instanceof Bundle && valueTwo instanceof Bundle &&
                    !equalBundles((Bundle) valueOne, (Bundle) valueTwo)) {
                return false;
            }
            else if(valueOne == null) {
                if(valueTwo != null || !two.containsKey(key))
                    return false;
            }
            else if(!valueOne.equals(valueTwo))
                return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_FILTER) {
            if (resultCode == RESULT_OK) {
                title = data.getStringExtra("extra_filter_name");
                query = data.getStringExtra("extra_filter_query");
                values = data.getStringExtra("extra_filter_values");
                updateActivity();
            }

            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PluginBundleValues.BUNDLE_EXTRA_PREVIOUS_BUNDLE, previousBundle);
        outState.putString(PluginBundleValues.BUNDLE_EXTRA_STRING_TITLE, title);
        outState.putString(PluginBundleValues.BUNDLE_EXTRA_STRING_QUERY, query);
        outState.putString(PluginBundleValues.BUNDLE_EXTRA_STRING_VALUES, values);
    }

    private void updateActivity() {
        ((TextView) findViewById(R.id.text_view)).setText(title);
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }
}
