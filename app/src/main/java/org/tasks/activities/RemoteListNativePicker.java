package org.tasks.activities;

import static org.tasks.activities.RemoteListSupportPicker.createDialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.filters.FilterProvider;
import org.tasks.gtasks.RemoteListSelectionHandler;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;

public class RemoteListNativePicker extends InjectingNativeDialogFragment {

  private static final String EXTRA_SELECTED = "extra_selected";

  @Inject DialogBuilder dialogBuilder;
  @Inject FilterAdapter filterAdapter;
  @Inject FilterProvider filterProvider;
  private RemoteListSelectionHandler handler;
  private CompositeDisposable disposables;

  public static RemoteListNativePicker newRemoteListNativePicker(Filter selected) {
    RemoteListNativePicker dialog = new RemoteListNativePicker();
    Bundle arguments = new Bundle();
    if (selected != null) {
      arguments.putParcelable(EXTRA_SELECTED, selected);
    }
    dialog.setArguments(arguments);
    return dialog;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      filterAdapter.restore(savedInstanceState);
    }

    return createDialog(filterAdapter, dialogBuilder, handler);
  }

  @Override
  public void onResume() {
    super.onResume();

    Filter selected = getArguments().getParcelable(EXTRA_SELECTED);

    disposables =
        new CompositeDisposable(
            Single.fromCallable(filterProvider::getRemoteListPickerItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(items -> filterAdapter.setData(items, selected, 0)));
  }

  @Override
  public void onPause() {
    super.onPause();

    disposables.dispose();
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    handler = (RemoteListSelectionHandler) activity;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    filterAdapter.save(outState);
  }

  @Override
  protected void inject(NativeDialogFragmentComponent component) {
    component.inject(this);
  }
}
