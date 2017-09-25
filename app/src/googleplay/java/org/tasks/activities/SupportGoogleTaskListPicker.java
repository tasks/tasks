package org.tasks.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.gtasks.GtasksListService;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.GoogleTaskListSelectionHandler;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

import java.util.List;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

public class SupportGoogleTaskListPicker extends InjectingDialogFragment {

    public static SupportGoogleTaskListPicker newSupportGoogleTaskListPicker(GtasksList selected) {
        SupportGoogleTaskListPicker dialog = new SupportGoogleTaskListPicker();
        Bundle arguments = new Bundle();
        if (selected != null) {
            arguments.putParcelable(EXTRA_SELECTED, selected.getStoreObject());
        }
        dialog.setArguments(arguments);
        return dialog;
    }

    private static final String EXTRA_SELECTED = "extra_selected";

    @Inject DialogBuilder dialogBuilder;
    @Inject GtasksListService gtasksListService;
    @Inject ThemeCache themeCache;

    private GoogleTaskListSelectionHandler handler;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        StoreObject storeObject = arguments.getParcelable(EXTRA_SELECTED);
        GtasksList selected = null;
        if (storeObject != null) {
            selected = new GtasksList(storeObject);
        }
        return createDialog(getActivity(), themeCache, dialogBuilder, gtasksListService,
                selected, list -> handler.selectedList(list));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        handler = (GoogleTaskListSelectionHandler) activity;
    }

    public static AlertDialog createDialog(Context context, ThemeCache themeCache,
                                           DialogBuilder dialogBuilder, GtasksListService gtasksListService,
                                           GtasksList selected, final GoogleTaskListSelectionHandler handler) {
        final List<GtasksList> lists = gtasksListService.getLists();
        ArrayAdapter<GtasksList> adapter = new ArrayAdapter<GtasksList>(context, R.layout.simple_list_item_single_choice_themed, lists) {
            @SuppressLint("NewApi")
            @NonNull
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
                GtasksList list = lists.get(position);
                if (selected != null && list.getRemoteId().equals(selected.getRemoteId())) {
                    view.setCheckMarkDrawable(R.drawable.ic_check_white_24dp);
                    view.setChecked(true);
                } else {
                    view.setCheckMarkDrawable(null);
                    view.setChecked(false);
                }
                int color = list.getColor();
                ThemeColor themeColor = themeCache.getThemeColor(color >= 0 ? color : 19);
                view.setText(list.getName());
                Drawable original = ContextCompat.getDrawable(getContext(), R.drawable.ic_cloud_black_24dp);
                Drawable wrapped = DrawableCompat.wrap(original.mutate());
                DrawableCompat.setTint(wrapped, themeColor.getPrimaryColor());
                if (atLeastJellybeanMR1()) {
                    view.setCompoundDrawablesRelativeWithIntrinsicBounds(wrapped, null, null, null);
                } else {
                    view.setCompoundDrawablesWithIntrinsicBounds(wrapped, null, null, null);
                }
                return view;
            }
        };
        return dialogBuilder.newDialog()
                .setSingleChoiceItems(adapter, -1, (dialog, which) -> {
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
