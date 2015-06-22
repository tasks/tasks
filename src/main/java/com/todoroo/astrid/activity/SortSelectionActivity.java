/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;

import com.todoroo.astrid.core.SortHelper;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastHoneycomb;

/**
 * Shows the sort / hidden dialog
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SortSelectionActivity {

    public interface OnSortSelectedListener {
        void onSortSelected(boolean always, int flags, int sort);
    }

    /**
     * Create the dialog
     */
    public static AlertDialog createDialog(Activity activity, boolean showDragDrop, ActivityPreferences activityPreferences,
            OnSortSelectedListener listener, int flags, int sort) {
        int editDialogTheme = activityPreferences.getEditDialogTheme();
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(activity, editDialogTheme);
        LayoutInflater themedInflater = activity.getLayoutInflater().cloneInContext(contextThemeWrapper);
        View body = themedInflater.inflate(R.layout.sort_selection_dialog, null);

        if((flags & SortHelper.FLAG_REVERSE_SORT) > 0) {
            ((CheckBox) body.findViewById(R.id.reverse)).setChecked(true);
        }
        if((flags & SortHelper.FLAG_SHOW_COMPLETED) > 0) {
            ((CheckBox) body.findViewById(R.id.completed)).setChecked(true);
        }
        if((flags & SortHelper.FLAG_SHOW_HIDDEN) > 0) {
            ((CheckBox) body.findViewById(R.id.hidden)).setChecked(true);
        }

        if(!showDragDrop) {
            body.findViewById(R.id.sort_drag).setVisibility(View.GONE);
        }

        if(showDragDrop && (flags & SortHelper.FLAG_DRAG_DROP) > 0) {
            ((RadioButton) body.findViewById(R.id.sort_drag)).setChecked(true);
        } else {
            switch(sort) {
            case SortHelper.SORT_ALPHA:
                ((RadioButton)body.findViewById(R.id.sort_alpha)).setChecked(true);
                break;
            case SortHelper.SORT_DUE:
                ((RadioButton)body.findViewById(R.id.sort_due)).setChecked(true);
                break;
            case SortHelper.SORT_IMPORTANCE:
                ((RadioButton)body.findViewById(R.id.sort_importance)).setChecked(true);
                break;
            case SortHelper.SORT_MODIFIED:
                ((RadioButton)body.findViewById(R.id.sort_modified)).setChecked(true);
                break;
            default:
                ((RadioButton)body.findViewById(R.id.sort_smart)).setChecked(true);
            }
        }

        body.findViewById(R.id.sort_drag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable reverse
            }
        });

        AlertDialog.Builder builder = atLeastHoneycomb()
                ? new AlertDialog.Builder(activity, editDialogTheme)
                : new AlertDialog.Builder(activity);
        AlertDialog dialog = builder.
            setTitle(R.string.TLA_menu_sort).
            setView(body).
            setPositiveButton(R.string.SSD_save_always,
                    new DialogOkListener(body, listener, true)).
            setNegativeButton(R.string.SSD_save_temp,
                    new DialogOkListener(body, listener, false)).
            create();
        dialog.setOwnerActivity(activity);
        return dialog;
    }

    // --- internal implementation

    private SortSelectionActivity() {
        // use the static method
    }

    private static class DialogOkListener implements OnClickListener {
        private final OnSortSelectedListener listener;
        private final boolean always;
        private final View body;

        public DialogOkListener(View body, OnSortSelectedListener listener, boolean always) {
            this.body = body;
            this.listener = listener;
            this.always = always;
        }

        @Override
        public void onClick(DialogInterface view, int button) {
            int flags = 0;
            int sort;

            if(((CheckBox)body.findViewById(R.id.reverse)).isChecked()) {
                flags |= SortHelper.FLAG_REVERSE_SORT;
            }
            if(((CheckBox)body.findViewById(R.id.completed)).isChecked()) {
                flags |= SortHelper.FLAG_SHOW_COMPLETED;
            }
            if(((CheckBox)body.findViewById(R.id.hidden)).isChecked()) {
                flags |= SortHelper.FLAG_SHOW_HIDDEN;
            }
            if(((RadioButton)body.findViewById(R.id.sort_drag)).isChecked()) {
                flags |= SortHelper.FLAG_DRAG_DROP;
            }

            if(((RadioButton)body.findViewById(R.id.sort_alpha)).isChecked()) {
                sort = SortHelper.SORT_ALPHA;
            } else if(((RadioButton)body.findViewById(R.id.sort_due)).isChecked()) {
                sort = SortHelper.SORT_DUE;
            } else if(((RadioButton)body.findViewById(R.id.sort_importance)).isChecked()) {
                sort = SortHelper.SORT_IMPORTANCE;
            } else if(((RadioButton)body.findViewById(R.id.sort_modified)).isChecked()) {
                sort = SortHelper.SORT_MODIFIED;
            } else {
                sort = SortHelper.SORT_AUTO;
            }

            listener.onSortSelected(always, flags, sort);
        }
    }
}
