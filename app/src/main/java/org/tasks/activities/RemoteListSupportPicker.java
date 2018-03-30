package org.tasks.activities;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

  private static final String EXTRA_SELECTED = "extra_selected";
  @Inject DialogBuilder dialogBuilder;
  @Inject FilterAdapter filterAdapter;
  private RemoteListSelectionHandler handler;

  public static RemoteListSupportPicker newRemoteListSupportPicker(Filter selected) {
    RemoteListSupportPicker dialog = new RemoteListSupportPicker();
    Bundle arguments = new Bundle();
    if (selected != null) {
      arguments.putParcelable(EXTRA_SELECTED, selected);
    }
    dialog.setArguments(arguments);
    return dialog;
  }

  public static AlertDialog createDialog(
      FilterAdapter filterAdapter,
      DialogBuilder dialogBuilder,
      Filter selected,
      RemoteListSelectionHandler handler) {
    filterAdapter.populateRemoteListPicker();
    int selectedIndex = selected == null ? 0 : filterAdapter.indexOf(selected);
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
    Bundle arguments = getArguments();
    Filter selected = arguments == null ? null : arguments.getParcelable(EXTRA_SELECTED);
    return createDialog(filterAdapter, dialogBuilder, selected, list -> handler.selectedList(list));
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    handler = (RemoteListSelectionHandler) activity;
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }
}
