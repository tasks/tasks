package com.todoroo.astrid.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;

import com.timsu.astrid.R;

public class SortSelectionActivity {

    private SortSelectionActivity() {
        // use the static method
    }

    /**
     * Create the dialog
     * @param activity
     * @return
     */
    public static AlertDialog createDialog(Activity activity) {
        View body = activity.getLayoutInflater().inflate(R.layout.sort_selection_dialog, null);

        AlertDialog dialog = new AlertDialog.Builder(activity).
            setTitle(R.string.SSD_title).
            setIcon(android.R.drawable.ic_menu_sort_by_size).
            setView(body).
            setPositiveButton(R.string.SSD_save_always, null).
            setNegativeButton(R.string.SSD_save_temp, null).
            create();
        dialog.setOwnerActivity(activity);
        return dialog;
    }

}
