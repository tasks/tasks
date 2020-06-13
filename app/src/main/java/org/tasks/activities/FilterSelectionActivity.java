package org.tasks.activities;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.filters.FilterProvider;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ApplicationContext;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.widget.WidgetPreferences;

public class FilterSelectionActivity extends InjectingAppCompatActivity {

  public static final String EXTRA_RETURN_FILTER = "extra_include_filter";
  public static final String EXTRA_FILTER = "extra_filter";
  private static final String EXTRA_FILTER_NAME = "extra_filter_name";
  private static final String EXTRA_FILTER_SQL = "extra_filter_query";
  private static final String EXTRA_FILTER_VALUES = "extra_filter_values";

  @Inject @ApplicationContext Context context;
  @Inject DialogBuilder dialogBuilder;
  @Inject FilterAdapter filterAdapter;
  @Inject FilterProvider filterProvider;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject Preferences preferences;
  @Inject DefaultFilterProvider defaultFilterProvider;

  private CompositeDisposable disposables;
  private Filter selected;
  private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      refresh();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    boolean returnFilter = intent.getBooleanExtra(EXTRA_RETURN_FILTER, false);
    int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
    selected = intent.getParcelableExtra(EXTRA_FILTER);

    if (savedInstanceState != null) {
      filterAdapter.restore(savedInstanceState);
    }

    dialogBuilder
        .newDialog()
        .setSingleChoiceItems(
            filterAdapter,
            -1,
            (dialog, which) -> {
              final Filter selectedFilter = (Filter) filterAdapter.getItem(which);
              Intent data = new Intent();
              if (returnFilter) {
                data.putExtra(EXTRA_FILTER, selectedFilter);
              }
              if (widgetId != -1) {
                new WidgetPreferences(context, preferences, widgetId)
                    .setFilter(defaultFilterProvider.getFilterPreferenceValue(selectedFilter));
                localBroadcastManager.reconfigureWidget(widgetId);
              }
              data.putExtra(EXTRA_FILTER_NAME, selectedFilter.listingTitle);
              data.putExtra(EXTRA_FILTER_SQL, selectedFilter.getSqlQuery());
              if (selectedFilter.valuesForNewTasks != null) {
                data.putExtra(
                    EXTRA_FILTER_VALUES,
                    AndroidUtilities.mapToSerializedString(selectedFilter.valuesForNewTasks));
              }
              setResult(RESULT_OK, data);
              dialog.dismiss();
            })
        .setOnDismissListener(dialog -> finish())
        .show();
  }

  @Override
  protected void onResume() {
    super.onResume();

    disposables = new CompositeDisposable();

    localBroadcastManager.registerRefreshListReceiver(refreshReceiver);

    refresh();
  }

  @Override
  protected void onPause() {
    super.onPause();

    localBroadcastManager.unregisterReceiver(refreshReceiver);

    disposables.dispose();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    filterAdapter.save(outState);
  }

  private void refresh() {
    disposables.add(
        Single.fromCallable(() -> filterProvider.getFilterPickerItems())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(items -> filterAdapter.setData(items, selected)));
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
