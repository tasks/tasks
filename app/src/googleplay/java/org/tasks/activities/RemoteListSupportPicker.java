package org.tasks.activities;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.GtasksFilter;

import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.RemoteListSelectionHandler;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;

import javax.inject.Inject;

public class RemoteListSupportPicker extends InjectingDialogFragment {

    public static RemoteListSupportPicker newRemoteListSupportPicker(Filter selected) {
        RemoteListSupportPicker dialog = new RemoteListSupportPicker();
        Bundle arguments = new Bundle();
        if (selected != null) {
            arguments.putParcelable(EXTRA_SELECTED, selected);
        }
        dialog.setArguments(arguments);
        return dialog;
    }

    private static final String EXTRA_SELECTED = "extra_selected";

    @Inject DialogBuilder dialogBuilder;
    @Inject FilterAdapter filterAdapter;

    private RemoteListSelectionHandler handler;

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

    public static AlertDialog createDialog(FilterAdapter filterAdapter, DialogBuilder dialogBuilder,
                                           Filter selected, RemoteListSelectionHandler handler) {
        filterAdapter.populateRemoteListPicker();
        int selectedIndex = filterAdapter.indexOf(selected);
        return dialogBuilder.newDialog()
                .setSingleChoiceItems(filterAdapter, selectedIndex, (dialog, which) -> {
                    FilterListItem item = filterAdapter.getItem(which);
                    if (item instanceof GtasksFilter) {
                        handler.selectedList((GtasksFilter) item);
                    }
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }
}
