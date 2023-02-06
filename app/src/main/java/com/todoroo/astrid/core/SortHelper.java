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
import com.todoroo.astrid.data.Task;

import org.tasks.preferences.QueryPreferences;

import java.util.Locale;

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
  public static final int SORT_CREATED = 5;
  public static final int SORT_GTASKS = 6;
  public static final int SORT_CALDAV = 7;
  public static final int SORT_START = 8;

  public static final long APPLE_EPOCH = 978307200000L; // 1/1/2001 GMT
  @SuppressLint("DefaultLocale")
  public static final String CALDAV_ORDER_COLUMN =
      String.format(Locale.US, "IFNULL(tasks.`order`, (tasks.created - %d) / 1000)", APPLE_EPOCH);

  private static final String ADJUSTED_DUE_DATE =
      "(CASE WHEN (dueDate / 1000) % 60 > 0 THEN dueDate ELSE (dueDate + 43140000) END)";
  private static final String ADJUSTED_START_DATE =
      "(CASE WHEN (hideUntil / 1000) % 60 > 0 THEN hideUntil ELSE (hideUntil + 86399000) END)";
  private static final Order ORDER_TITLE = Order.asc(Functions.upper(Task.TITLE));

  /** Takes a SQL query, and if there isn't already an order, creates an order. */
  public static String adjustQueryForFlagsAndSort(
      QueryPreferences preferences, String originalSql, int sort) {
    // sort
    if (originalSql == null) {
      originalSql = "";
    }
    if (!originalSql.toUpperCase().contains("ORDER BY")) {
      Order order = orderForSortType(sort);

      if (preferences.isReverseSort()) {
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
        order =
            Order.asc(
                "importance*(strftime('%s','now')*1000)+(CASE WHEN (dueDate=0) THEN (strftime('%s','now')*1000) ELSE dueDate END)");
        break;
      case SORT_MODIFIED:
        order = Order.desc(Task.MODIFICATION_DATE);
        break;
      case SORT_CREATED:
        order = Order.desc(Task.CREATION_DATE);
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
      default:
        return null;
    }
  }

  public static String orderSelectForSortTypeRecursive(int sortType) {
    String select;
    switch (sortType) {
      case SORT_ALPHA:
        // Return an empty string, providing a value to fill the WITH clause template
        select = "''";
        break;
      case SORT_DUE:
        select = "(CASE WHEN (tasks.dueDate=0) THEN (strftime('%s','now')*1000)*2 ELSE "
                   + ADJUSTED_DUE_DATE.replace("dueDate", "tasks.dueDate")
                   + " END)+tasks.importance AS sort_duedate";
        break;
      case SORT_START:
        select = "(CASE WHEN (tasks.hideUntil=0) THEN (strftime('%s','now')*1000)*2 ELSE "
                  + ADJUSTED_START_DATE.replace("hideUntil", "tasks.hideUntil")
                  + " END)+tasks.importance AS sort_startdate";
        break;
      case SORT_IMPORTANCE:
        select = "tasks.importance*(strftime('%s','now')*1000)+(CASE WHEN (tasks.dueDate=0) THEN (strftime('%s','now')*1000) ELSE tasks.dueDate END) AS sort_importance";
        break;
      case SORT_MODIFIED:
        select = "tasks.modified AS sort_modified";
        break;
      case SORT_CREATED:
        select = "tasks.created AS sort_created";
        break;
      case SORT_GTASKS:
        select = "tasks.`order` AS sort_manual";
        break;
      case SORT_CALDAV:
        select = CALDAV_ORDER_COLUMN + " AS sort_manual";
        break;
      default:
        select ="(CASE WHEN (tasks.dueDate=0) "
                    + // if no due date
                    "THEN (strftime('%s','now')*1000)*2 "
                    + // then now * 2
                    "ELSE ("
                    + ADJUSTED_DUE_DATE.replace("dueDate", "tasks.dueDate")
                    + ") END) "
                    + // else due time
					  // add slightly less than 2 days * importance to give due date priority over importance in case of tie
                    "+ 172799999 * tasks.importance AS sort_smart"; 
    }

    return select;
  }

  public static Order orderForSortTypeRecursive(int sortMode, boolean reverse) {
    Order order;
    switch (sortMode) {
      case SORT_ALPHA:
        order = Order.asc("sort_title");
        break;
      case SORT_DUE:
        order = Order.asc("sort_duedate");
        break;
      case SORT_START:
        order = Order.asc("sort_startdate");
        break;
      case SORT_IMPORTANCE:
        order = Order.asc("sort_importance");
        break;
      case SORT_MODIFIED:
        order = Order.desc("sort_modified");
        break;
      case SORT_CREATED:
        order = Order.desc("sort_created");
        break;
      case SORT_GTASKS:
      case SORT_CALDAV:
        order = Order.asc("sort_manual");
        break;
      default:
        order = Order.asc("sort_smart");
    }
    if (sortMode != SORT_ALPHA) {
      order.addSecondaryExpression(Order.asc("sort_title"));
    }

    if (reverse) {
      order = order.reverse();
    }

    return order;
  }
}
