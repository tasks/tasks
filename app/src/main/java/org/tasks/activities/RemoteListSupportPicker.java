package org.tasks.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.GtasksFilter;
import javax.inject.Inject;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.RemoteListSelectionHandler;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;

public class RemoteListSupportPicker extends InjectingDialogFragment {

  public static final String EXTRA_SELECTED_FILTER = "extra_selected_filter";
  private static final String EXTRA_NO_SELECTION = "extra_no_selection";

  @Inject DialogBuilder dialogBuilder;
  @Inject FilterAdapter filterAdapter;

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

  public static AlertDialog createDialog(
      FilterAdapter filterAdapter,
      DialogBuilder dialogBuilder,
      int selectedIndex,
      RemoteListSelectionHandler handler) {
    return dialogBuilder
        .newDialog()
        .setSingleChoiceItems(
            filterAdapter,
            selectedIndex,
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
    filterAdapter.populateRemoteListPicker();
    Bundle arguments = getArguments();
    int selected =
        arguments.getBoolean(EXTRA_NO_SELECTION, false)
            ? -1
            : filterAdapter.indexOf(arguments.getParcelable(EXTRA_SELECTED_FILTER), 0);
    return createDialog(filterAdapter, dialogBuilder, selected, this::selected);
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
