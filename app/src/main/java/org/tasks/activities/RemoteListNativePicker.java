package org.tasks.activities;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;

import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.RemoteListSelectionHandler;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;

import javax.inject.Inject;

import static org.tasks.activities.RemoteListSupportPicker.createDialog;

public class RemoteListNativePicker extends InjectingNativeDialogFragment {

    public static RemoteListNativePicker newRemoteListNativePicker(Filter selected) {
        RemoteListNativePicker dialog = new RemoteListNativePicker();
        Bundle arguments = new Bundle();
        if (selected != null) {
            arguments.putParcelable(EXTRA_SELECTED, selected);
        }
        dialog.setArguments(arguments);
        return dialog;
    }

    public static final String EXTRA_SELECTED = "extra_selected";

    @Inject DialogBuilder dialogBuilder;
    @Inject FilterAdapter filterAdapter;

    private RemoteListSelectionHandler handler;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        Filter selected = arguments.getParcelable(EXTRA_SELECTED);
        return createDialog(filterAdapter, dialogBuilder, selected, list -> handler.selectedList(list));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        handler = (RemoteListSelectionHandler) activity;
    }

    @Override
    protected void inject(NativeDialogFragmentComponent component) {
        component.inject(this);
    }
}
