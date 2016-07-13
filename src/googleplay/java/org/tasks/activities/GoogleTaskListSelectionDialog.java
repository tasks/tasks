package org.tasks.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

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

public class GoogleTaskListSelectionDialog extends InjectingDialogFragment {

    @Inject DialogBuilder dialogBuilder;
    @Inject GtasksListService gtasksListService;

    private GoogleTaskListSelectionHandler handler;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<GtasksList> lists = gtasksListService.getLists();
        List<String> listNames = transform(lists, new Function<GtasksList, String>() {
            @Override
            public String apply(GtasksList gtasksList) {
                return gtasksList.getName();
            }
        });
        return dialogBuilder.newDialog()
                .setItems(listNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.selectedList(lists.get(which));
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        handler = (GoogleTaskListSelectionHandler) activity;
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }
}
