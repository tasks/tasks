package org.tasks.activities;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import com.todoroo.astrid.gtasks.GtasksListService;

import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.GoogleTaskListSelectionHandler;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;
import org.tasks.themes.ThemeCache;

import javax.inject.Inject;

import static org.tasks.activities.SupportGoogleTaskListPicker.createDialog;

public class NativeGoogleTaskListPicker extends InjectingNativeDialogFragment {

    @Inject DialogBuilder dialogBuilder;
    @Inject GtasksListService gtasksListService;
    @Inject ThemeCache themeCache;

    private GoogleTaskListSelectionHandler handler;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return createDialog(getActivity(), themeCache, dialogBuilder, gtasksListService, list -> handler.selectedList(list));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        handler = (GoogleTaskListSelectionHandler) activity;
    }

    @Override
    protected void inject(NativeDialogFragmentComponent component) {
        component.inject(this);
    }
}
