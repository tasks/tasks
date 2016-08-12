package org.tasks.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.google.common.base.Function;
import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.gtasks.GtasksListService;

import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.GoogleTaskListSelectionHandler;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;

import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Lists.transform;

public class SupportGoogleTaskListPicker extends InjectingDialogFragment {

    @Inject DialogBuilder dialogBuilder;
    @Inject GtasksListService gtasksListService;

    private GoogleTaskListSelectionHandler handler;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return createDialog(dialogBuilder, gtasksListService, list -> handler.selectedList(list));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        handler = (GoogleTaskListSelectionHandler) activity;
    }

    public static AlertDialog createDialog(DialogBuilder dialogBuilder, GtasksListService gtasksListService, final GoogleTaskListSelectionHandler handler) {
        final List<GtasksList> lists = gtasksListService.getLists();
        List<String> listNames = transform(lists, GtasksList::getName);
        return dialogBuilder.newDialog()
                .setItems(listNames, (dialog, which) -> {
                    handler.selectedList(lists.get(which));
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }
}
