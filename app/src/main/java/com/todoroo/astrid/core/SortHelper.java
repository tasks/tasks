/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.isVisible;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import org.tasks.R;
import org.tasks.preferences.Preferences;

/**
 * Helpers for sorting a list of tasks
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class SortHelper {

  public static final int SORT_AUTO = 0;
  public static final int SORT_ALPHA = 1;
  public static final int SORT_DUE = 2;
  public static final int SORT_IMPORTANCE = 3;
  public static final int SORT_MODIFIED = 4;
  public static final int SORT_WIDGET = 5;

  private static final Order ORDER_TITLE = Order.asc(Functions.upper(Task.TITLE));

  /** Takes a SQL query, and if there isn't already an order, creates an order. */
  public static String adjustQueryForFlagsAndSort(
      Preferences preferences, String originalSql, int sort) {
    // sort
    if (originalSql == null) {
      originalSql = "";
    }
    if (!originalSql.toUpperCase().contains("ORDER BY")) {
      Order order = orderForSortType(sort);

      if (preferences.getBoolean(R.string.p_reverse_sort, false)) {
        order = order.reverse();
      }
      originalSql += " ORDER BY " + order;
    }

    // flags
    if (preferences.getBoolean(R.string.p_show_completed_tasks, false)) {
      originalSql =
          originalSql.replace(Task.COMPLETION_DATE.eq(0).toString(), Criterion.all.toString());
    } else {
      originalSql =
          originalSql.replace(
              Task.COMPLETION_DATE.eq(0).toString(),
              Criterion.or(
                      Task.COMPLETION_DATE.lte(0),
                      Task.COMPLETION_DATE.gt(DateUtilities.now() - 60000))
                  .toString());
    }
    if (preferences.getBoolean(R.string.p_show_hidden_tasks, false)) {
      originalSql = originalSql.replace(isVisible().toString(), Criterion.all.toString());
    }

    return originalSql;
  }

  private static Order orderForSortType(int sortType) {
    Order order;
    switch (sortType) {
      case SORT_ALPHA:
        order = ORDER_TITLE;
        break;
      case SORT_DUE:
        order =
            Order.asc(
                "(CASE WHEN (dueDate=0) THEN (strftime('%s','now')*1000)*2 ELSE (CASE WHEN (dueDate / 60000) > 0 THEN dueDate ELSE (dueDate + 43140000) END) END)+importance");
        break;
      case SORT_IMPORTANCE:
        order =
            Order.asc(
                "importance*(strftime('%s','now')*1000)+(CASE WHEN (dueDate=0) THEN (strftime('%s','now')*1000) ELSE dueDate END)");
        break;
      case SORT_MODIFIED:
        order = Order.desc(Task.MODIFICATION_DATE);
        break;
      case SORT_WIDGET:
      default:
        order =
            Order.asc(
                "(CASE WHEN (dueDate=0) "
                    + // if no due date
                    "THEN (strftime('%s','now')*1000)*2 "
                    + // then now * 2
                    "ELSE ("
                    + adjustedDueDateFunction()
                    + ") END) "
                    + // else due time
                    "+ 172800000 * importance"); // add 2 days * importance
    }
    if (sortType != SORT_ALPHA) {
      order.addSecondaryExpression(ORDER_TITLE);
    }

    return order;
  }

  private static String adjustedDueDateFunction() {
    // if no due time use 11:59AM
    return "(CASE WHEN (dueDate / 60000) > 0 THEN dueDate ELSE (dueDate + 43140000) END)";
  }
}
