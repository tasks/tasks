package org.tasks.activities;

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
import org.tasks.dialogs.DialogBuilder;
import org.tasks.filters.FilterProvider;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;

public class FilterSelectionActivity extends InjectingAppCompatActivity {

  public static final String EXTRA_RETURN_FILTER = "extra_include_filter";
  public static final String EXTRA_FILTER = "extra_filter";
  private static final String EXTRA_FILTER_NAME = "extra_filter_name";
  private static final String EXTRA_FILTER_SQL = "extra_filter_query";
  private static final String EXTRA_FILTER_VALUES = "extra_filter_values";

  @Inject DialogBuilder dialogBuilder;
  @Inject FilterAdapter filterAdapter;
  @Inject FilterProvider filterProvider;

  private CompositeDisposable disposables;
  private Filter selected;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    boolean returnFilter = intent.getBooleanExtra(EXTRA_RETURN_FILTER, false);
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
    disposables.add(
        Single.fromCallable(() -> filterProvider.getItems(false))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(items -> filterAdapter.setData(items, selected)));
  }

  @Override
  protected void onPause() {
    super.onPause();

    disposables.dispose();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    filterAdapter.save(outState);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
