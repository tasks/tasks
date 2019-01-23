package org.tasks.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.GtasksFilter;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.filters.FilterProvider;
import org.tasks.gtasks.RemoteListSelectionHandler;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;

public class RemoteListSupportPicker extends InjectingDialogFragment {

  public static final String EXTRA_SELECTED_FILTER = "extra_selected_filter";
  private static final String EXTRA_NO_SELECTION = "extra_no_selection";

  @Inject DialogBuilder dialogBuilder;
  @Inject FilterAdapter filterAdapter;
  @Inject FilterProvider filterProvider;

  private CompositeDisposable disposables;

  public static RemoteListSupportPicker newRemoteListSupportPicker(
      Filter selected, Fragment targetFragment, int requestCode) {
    RemoteListSupportPicker dialog = new RemoteListSupportPicker();
    Bundle arguments = new Bundle();
    arguments.putParcelable(EXTRA_SELECTED_FILTER, selected);
    dialog.setArguments(arguments);
    dialog.setTargetFragment(targetFragment, requestCode);
    return dialog;
  }

  public static RemoteListSupportPicker newRemoteListSupportPicker(
      Fragment targetFragment, int requestCode) {
    RemoteListSupportPicker dialog = new RemoteListSupportPicker();
    Bundle arguments = new Bundle();
    arguments.putBoolean(EXTRA_NO_SELECTION, true);
    dialog.setArguments(arguments);
    dialog.setTargetFragment(targetFragment, requestCode);
    return dialog;
  }

  static AlertDialog createDialog(
      FilterAdapter filterAdapter,
      DialogBuilder dialogBuilder,
      RemoteListSelectionHandler handler) {
    return dialogBuilder
        .newDialog()
        .setSingleChoiceItems(
            filterAdapter,
            -1,
            (dialog, which) -> {
              if (which == 0) {
                handler.selectedList(null);
              } else {
                FilterListItem item = filterAdapter.getItem(which);
                if (item instanceof GtasksFilter || item instanceof CaldavFilter) {
                  handler.selectedList((Filter) item);
                }
              }
              dialog.dismiss();
            })
        .show();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      filterAdapter.restore(savedInstanceState);
    }

    return createDialog(filterAdapter, dialogBuilder, this::selected);
  }

  @Override
  public void onResume() {
    super.onResume();

    Bundle arguments = getArguments();
    boolean noSelection = arguments.getBoolean(EXTRA_NO_SELECTION, false);
    Filter selected = noSelection ? null : arguments.getParcelable(EXTRA_SELECTED_FILTER);

    disposables =
        new CompositeDisposable(
            Single.fromCallable(filterProvider::getRemoteListPickerItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(items -> filterAdapter.setData(items, selected, noSelection ? -1 : 0)));
  }

  @Override
  public void onPause() {
    super.onPause();

    disposables.dispose();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    filterAdapter.save(outState);
  }

  private void selected(Filter filter) {
    getTargetFragment()
        .onActivityResult(
            getTargetRequestCode(),
            Activity.RESULT_OK,
            new Intent().putExtra(EXTRA_SELECTED_FILTER, filter));
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }
}
