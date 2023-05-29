/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.core;

import static org.tasks.db.QueryUtils.showCompleted;
import static org.tasks.db.QueryUtils.showHidden;

import android.annotation.SuppressLint;

import androidx.annotation.Nullable;

import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.OrderType;
import com.todoroo.astrid.data.Task;

import org.tasks.data.CaldavCalendar;
import org.tasks.preferences.QueryPreferences;

import java.util.Locale;

/**
 * Helpers for sorting a list of tasks
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class SortHelper {

  public static final int GROUP_NONE = -1;
  public static final int SORT_AUTO = 0;
  public static final int SORT_ALPHA = 1;
  public static final int SORT_DUE = 2;
  public static final int SORT_IMPORTANCE = 3;
  public static final int SORT_MODIFIED = 4;
  public static final int SORT_CREATED = 5;
  public static final int SORT_GTASKS = 6;
  public static final int SORT_CALDAV = 7;
  public static final int SORT_START = 8;
  public static final int SORT_LIST = 9;
  public static final int SORT_COMPLETED = 10;
  public static final int SORT_MANUAL = 11;

  public static final long APPLE_EPOCH = 978307200000L; // 1/1/2001 GMT
  @SuppressLint("DefaultLocale")
  public static final String CALDAV_ORDER_COLUMN =
      String.format(Locale.US, "IFNULL(tasks.`order`, (tasks.created - %d) / 1000)", APPLE_EPOCH);

  private static final String ADJUSTED_DUE_DATE =
      "(CASE WHEN (dueDate / 1000) % 60 > 0 THEN dueDate ELSE (dueDate + 43140000) END)";
  private static final String ADJUSTED_START_DATE =
      "(CASE WHEN (hideUntil / 1000) % 60 > 0 THEN hideUntil ELSE (hideUntil + 86399000) END)";

  private static final Long NO_DATE = 3538339200000L;

  private static final String GROUP_DUE_DATE = "((CASE WHEN (tasks.dueDate=0) THEN " + NO_DATE + " ELSE "
          + "tasks.dueDate END)+tasks.importance * 1000)";

  private static final String SORT_DUE_DATE = "((CASE WHEN (tasks.dueDate=0) THEN " + NO_DATE + " ELSE "
          + ADJUSTED_DUE_DATE.replace("dueDate", "tasks.dueDate")
          + " END)+tasks.importance * 1000)";

  private static final String GROUP_START_DATE = "((CASE WHEN (tasks.hideUntil=0) THEN " + NO_DATE + " ELSE "
          + "tasks.hideUntil END)+tasks.importance * 1000)";

  private static final String SORT_START_DATE = "((CASE WHEN (tasks.hideUntil=0) THEN " + NO_DATE + " ELSE "
          + ADJUSTED_START_DATE.replace("hideUntil", "tasks.hideUntil")
          + " END)+tasks.importance * 1000)";

  private static final Order ORDER_TITLE = Order.asc(Functions.upper(Task.TITLE));
  private static final Order ORDER_LIST =
          Order.asc(Functions.upper(CaldavCalendar.ORDER))
                  .addSecondaryExpression(Order.asc(CaldavCalendar.NAME));

  /** Takes a SQL query, and if there isn't already an order, creates an order. */
  public static String adjustQueryForFlagsAndSort(
      QueryPreferences preferences, String originalSql, int sort) {
    // sort
    if (originalSql == null) {
      originalSql = "";
    }
    if (!originalSql.toUpperCase().contains("ORDER BY")) {
      Order order = orderForSortType(sort);

      if (order.getOrderType() == OrderType.ASC != preferences.getSortAscending()) {
        order = order.reverse();
      }
      originalSql += " ORDER BY " + order;
    }

    return adjustQueryForFlags(preferences, originalSql);
  }

  public static String adjustQueryForFlags(QueryPreferences preferences, String originalSql) {
    String adjustedSql = originalSql;

    // flags
    if (preferences.getShowCompleted()) {
      adjustedSql = showCompleted(adjustedSql);
    }
    if (preferences.getShowHidden()) {
      adjustedSql = showHidden(adjustedSql);
    }

    return adjustedSql;
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
                "(CASE WHEN (dueDate=0) THEN (strftime('%s','now')*1000)*2 ELSE "
                    + ADJUSTED_DUE_DATE
                    + " END)+importance");
        break;
      case SORT_START:
        order =
            Order.asc(
                "(CASE WHEN (hideUntil=0) THEN (strftime('%s','now')*1000)*2 ELSE "
                    + ADJUSTED_START_DATE
                    + " END)+importance");
        break;
      case SORT_IMPORTANCE:
        order = Order.asc("importance");
        break;
      case SORT_MODIFIED:
        order = Order.desc(Task.MODIFICATION_DATE);
        break;
      case SORT_CREATED:
        order = Order.desc(Task.CREATION_DATE);
        break;
      case SORT_LIST:
        order = ORDER_LIST;
        break;
      default:
        order =
            Order.asc(
                "(CASE WHEN (dueDate=0) "
                    + // if no due date
                    "THEN (strftime('%s','now')*1000)*2 "
                    + // then now * 2
                    "ELSE ("
                    + ADJUSTED_DUE_DATE
                    + ") END) "
                    + // else due time
					// add slightly less than 2 days * importance to give due date priority over importance in case of tie
                    "+ 172799999 * importance");
    }
    if (sortType != SORT_ALPHA) {
      order.addSecondaryExpression(ORDER_TITLE);
    }

    return order;
  }

  public static @Nullable String getSortGroup(int sortType) {
    switch (sortType) {
      case SORT_DUE:
        return "tasks.dueDate";
      case SORT_START:
        return "tasks.hideUntil";
      case SORT_IMPORTANCE:
        return "tasks.importance";
      case SORT_MODIFIED:
        return "tasks.modified";
      case SORT_CREATED:
        return "tasks.created";
      case SORT_LIST:
        return "cdl_id";
      default:
        return null;
    }
  }

  private static String sortGroup(String column) {
    return "datetime(" + column + " / 1000, 'unixepoch', 'localtime', 'start of day')";
  }

  public static String orderSelectForSortTypeRecursive(int sortType, boolean grouping) {
    return switch (sortType) {
      case GROUP_NONE -> "1";
      case SORT_ALPHA -> "UPPER(tasks.title)";
      case SORT_DUE -> grouping ? sortGroup(GROUP_DUE_DATE) : SORT_DUE_DATE;
      case SORT_START -> grouping ? sortGroup(GROUP_START_DATE) : SORT_START_DATE;
      case SORT_IMPORTANCE -> "tasks.importance";
      case SORT_MODIFIED -> grouping ? sortGroup("tasks.modified") : "tasks.modified";
      case SORT_CREATED -> grouping ? sortGroup("tasks.created") : "tasks.created";
      case SORT_GTASKS -> "tasks.`order`";
      case SORT_CALDAV -> CALDAV_ORDER_COLUMN;
      case SORT_LIST -> "CASE WHEN cdl_order = -1 THEN cdl_name ELSE cdl_order END";
      case SORT_COMPLETED -> "tasks.completed";
      default -> "(CASE WHEN (tasks.dueDate=0) "
              + // if no due date
              "THEN (strftime('%s','now')*1000)*2 "
              + // then now * 2
              "ELSE ("
              + ADJUSTED_DUE_DATE.replace("dueDate", "tasks.dueDate")
              + ") END) "
              + // else due time
              // add slightly less than 2 days * importance to give due date priority over importance in case of tie
              "+ 172799999 * tasks.importance";
    };
  }

  public static Order orderForGroupTypeRecursive(int groupMode, boolean ascending) {
    return ascending
            ? Order.asc("primary_group")
            : Order.desc("primary_group");
  }

  public static Order orderForSortTypeRecursive(
          int sortMode,
          boolean primaryAscending,
          int secondaryMode,
          boolean secondaryAscending
  ) {
    Order order = primaryAscending || sortMode == SORT_GTASKS || sortMode == SORT_CALDAV
            ? Order.asc("primary_sort")
            : Order.desc("primary_sort");
    order.addSecondaryExpression(
            secondaryAscending || secondaryMode == SORT_GTASKS || secondaryMode == SORT_CALDAV
                    ? Order.asc("secondary_sort")
                    : Order.desc("secondary_sort")
    );
    if (sortMode != SORT_ALPHA) {
      order.addSecondaryExpression(Order.asc("sort_title"));
    }
    return order;
  }
}
