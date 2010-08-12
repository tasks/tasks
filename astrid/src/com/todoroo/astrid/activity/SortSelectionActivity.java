package com.todoroo.astrid.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;

import com.timsu.astrid.R;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.TaskService;

/**
 * Shows the sort / hidden dialog
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SortSelectionActivity {

    public static final int FLAG_REVERSE_SORT = 1 << 0;
    public static final int FLAG_SHOW_COMPLETED = 1 << 1;
    public static final int FLAG_SHOW_HIDDEN = 1 << 2;
    public static final int FLAG_SHOW_DELETED = 1 << 3;

    public static final int SORT_AUTO = 0;
    public static final int SORT_ALPHA = 1;
    public static final int SORT_DUE = 2;
    public static final int SORT_IMPORTANCE = 3;
    public static final int SORT_MODIFIED = 4;

    public interface OnSortSelectedListener {
        public void onSortSelected(boolean always, int flags, int sort);
    }

    /**
     * Create the dialog
     * @param activity
     * @return
     */
    public static AlertDialog createDialog(Activity activity,
            OnSortSelectedListener listener, int flags, int sort) {
        View body = activity.getLayoutInflater().inflate(R.layout.sort_selection_dialog, null);

        if((flags & FLAG_REVERSE_SORT) > 0)
            ((CheckBox)body.findViewById(R.id.reverse)).setChecked(true);
        if((flags & FLAG_SHOW_COMPLETED) > 0)
            ((CheckBox)body.findViewById(R.id.completed)).setChecked(true);
        if((flags & FLAG_SHOW_HIDDEN) > 0)
            ((CheckBox)body.findViewById(R.id.hidden)).setChecked(true);
        if((flags & FLAG_SHOW_DELETED) > 0)
            ((CheckBox)body.findViewById(R.id.deleted)).setChecked(true);

        switch(sort) {
        case SORT_ALPHA:
            ((RadioButton)body.findViewById(R.id.sort_alpha)).setChecked(true);
            break;
        case SORT_DUE:
            ((RadioButton)body.findViewById(R.id.sort_due)).setChecked(true);
            break;
        case SORT_IMPORTANCE:
            ((RadioButton)body.findViewById(R.id.sort_importance)).setChecked(true);
            break;
        case SORT_MODIFIED:
            ((RadioButton)body.findViewById(R.id.sort_modified)).setChecked(true);
            break;
        default:
            ((RadioButton)body.findViewById(R.id.sort_smart)).setChecked(true);
        }

        AlertDialog dialog = new AlertDialog.Builder(activity).
            setTitle(R.string.SSD_title).
            setIcon(android.R.drawable.ic_menu_sort_by_size).
            setView(body).
            setPositiveButton(R.string.SSD_save_always,
                    new DialogOkListener(body, listener, true)).
            setNegativeButton(R.string.SSD_save_temp,
                    new DialogOkListener(body, listener, false)).
            create();
        dialog.setOwnerActivity(activity);
        return dialog;
    }

    @SuppressWarnings("nls")
    public static String adjustQueryForFlagsAndSort(String originalSql, int flags, int sort) {
        // sort
        if(!originalSql.toUpperCase().contains("ORDER BY")) {
            Order order;
            switch(sort) {
            case SortSelectionActivity.SORT_ALPHA:
                order = Order.asc(Functions.upper(Task.TITLE));
                break;
            case SortSelectionActivity.SORT_DUE:
                order = Order.asc(Functions.caseStatement(Task.DUE_DATE.eq(0),
                        DateUtilities.now()*2, Task.DUE_DATE) + "+" + Task.IMPORTANCE);
                break;
            case SortSelectionActivity.SORT_IMPORTANCE:
                order = Order.asc(Task.IMPORTANCE + "*" + (2*DateUtilities.now()) + //$NON-NLS-1$
                        "+" + Functions.caseStatement(Task.DUE_DATE.eq(0), //$NON-NLS-1$
                                Functions.now() + "+" + DateUtilities.ONE_WEEK, //$NON-NLS-1$
                                Task.DUE_DATE));
                break;
            case SortSelectionActivity.SORT_MODIFIED:
                order = Order.desc(Task.MODIFICATION_DATE);
                break;
            default:
                order = TaskService.defaultTaskOrder();
            }

            if((flags & SortSelectionActivity.FLAG_REVERSE_SORT) > 0)
                order = order.reverse();
            originalSql += " ORDER BY " + order;
        }

        // flags
        if((flags & FLAG_SHOW_COMPLETED) > 0)
            originalSql = originalSql.replace(Task.COMPLETION_DATE.eq(0).toString(),
                    Criterion.all.toString());
        if((flags & FLAG_SHOW_HIDDEN) > 0)
            originalSql = originalSql.replace(TaskCriteria.isVisible().toString(),
                    Criterion.all.toString());
        if((flags & FLAG_SHOW_DELETED) > 0)
            originalSql = originalSql.replace(Task.DELETION_DATE.eq(0).toString(),
                    Criterion.all.toString());

        return originalSql;
    }

    // --- internal implementation

    /** preference key for sort flags */
    public static final String PREF_SORT_FLAGS = "sort_flags"; //$NON-NLS-1$

    /** preference key for sort sort */
    public static final String PREF_SORT_SORT = "sort_sort"; //$NON-NLS-1$

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
            int sort = 0;

            if(((CheckBox)body.findViewById(R.id.reverse)).isChecked())
                flags |= FLAG_REVERSE_SORT;
            if(((CheckBox)body.findViewById(R.id.completed)).isChecked())
                flags |= FLAG_SHOW_COMPLETED;
            if(((CheckBox)body.findViewById(R.id.hidden)).isChecked())
                flags |= FLAG_SHOW_HIDDEN;
            if(((CheckBox)body.findViewById(R.id.deleted)).isChecked())
                flags |= FLAG_SHOW_DELETED;

            if(((RadioButton)body.findViewById(R.id.sort_alpha)).isChecked())
                sort = SORT_ALPHA;
            else if(((RadioButton)body.findViewById(R.id.sort_due)).isChecked())
                sort = SORT_DUE;
            else if(((RadioButton)body.findViewById(R.id.sort_importance)).isChecked())
                sort = SORT_IMPORTANCE;
            else if(((RadioButton)body.findViewById(R.id.sort_modified)).isChecked())
                sort = SORT_MODIFIED;
            else
                sort = SORT_AUTO;

            listener.onSortSelected(always, flags, sort);
        }
    }


}
