package com.todoroo.astrid.core;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao.TaskCriteria;

/**
 * Helpers for sorting a list of tasks
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SortHelper {

    public static final int FLAG_REVERSE_SORT = 1 << 0;
    public static final int FLAG_SHOW_COMPLETED = 1 << 1;
    public static final int FLAG_SHOW_HIDDEN = 1 << 2;
    public static final int FLAG_SHOW_DELETED = 1 << 3;

    public static final int SORT_AUTO = 0;
    public static final int SORT_ALPHA = 1;
    public static final int SORT_DUE = 2;
    public static final int SORT_IMPORTANCE = 3;
    public static final int SORT_MODIFIED = 4;

    /** preference key for sort flags. stored in public prefs */
    public static final String PREF_SORT_FLAGS = "sort_flags"; //$NON-NLS-1$

    /** preference key for sort sort. stored in public prefs */
    public static final String PREF_SORT_SORT = "sort_sort"; //$NON-NLS-1$

    /**
     * Takes a SQL query, and if there isn't already an order, creates an order.
     * @param originalSql
     * @param flags
     * @param sort
     * @return
     */
    @SuppressWarnings("nls")
    public static String adjustQueryForFlagsAndSort(String originalSql, int flags, int sort) {
        // sort
        if(originalSql == null)
            originalSql = "";
        if(!originalSql.toUpperCase().contains("ORDER BY")) {
            Order order = orderForSortType(sort);

            if((flags & FLAG_REVERSE_SORT) > 0)
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


    public static Order orderForSortType(int sortType) {
        Order order;
        switch(sortType) {
        case SORT_ALPHA:
            order = Order.asc(Functions.upper(Task.TITLE));
            break;
        case SORT_DUE:
            order = Order.asc(Functions.caseStatement(Task.DUE_DATE.eq(0),
                    DateUtilities.now()*2, Task.DUE_DATE) + "+" + Task.IMPORTANCE +
                    "+3*" + Task.COMPLETION_DATE);
            break;
        case SORT_IMPORTANCE:
            order = Order.asc(Task.IMPORTANCE + "*" + (2*DateUtilities.now()) + //$NON-NLS-1$
                    "+" + Functions.caseStatement(Task.DUE_DATE.eq(0), //$NON-NLS-1$
                            Functions.now() + "+" + DateUtilities.ONE_WEEK, //$NON-NLS-1$
                            Task.DUE_DATE) + "+8*" + Task.COMPLETION_DATE);
            break;
        case SORT_MODIFIED:
            order = Order.desc(Task.MODIFICATION_DATE);
            break;
        default:
            order = defaultTaskOrder();
        }
        return order;
    }

    /**
     * Returns SQL task ordering that is astrid's default algorithm
     * @return
     */
    @SuppressWarnings("nls")
    public static Order defaultTaskOrder() {
        return Order.asc(Functions.caseStatement(Task.DUE_DATE.eq(0),
                DateUtilities.now() + DateUtilities.ONE_WEEK,
                Task.DUE_DATE) + " + 200000000 * " +
                Task.IMPORTANCE + " + 2*" + Task.COMPLETION_DATE);
    }

}
