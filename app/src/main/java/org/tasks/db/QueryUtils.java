package org.tasks.db;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import java.util.regex.Pattern;

public class QueryUtils {

  private static final Pattern HIDDEN =
      Pattern.compile("tasks\\.hideUntil<=?\\(strftime\\('%s','now'\\)\\*1000\\)");

  private static final Pattern UNCOMPLETED = Pattern.compile("tasks\\.completed<?=0");

  private static final Pattern ORDER =
      Pattern.compile("order by .*? (asc|desc)", Pattern.CASE_INSENSITIVE);

  public static String showHidden(String query) {
    return HIDDEN.matcher(query).replaceAll("1");
  }

  public static String showCompleted(String query) {
    return UNCOMPLETED.matcher(query).replaceAll("1");
  }

  public static String showHiddenAndCompleted(String query) {
    return showCompleted(showHidden(query));
  }

  public static String showRecentlyCompleted(String query) {
    return UNCOMPLETED
        .matcher(query)
        .replaceAll(
            Criterion.or(
                    Task.COMPLETION_DATE.lte(0),
                    Task.COMPLETION_DATE.gte(DateUtilities.now() - 59999))
                .toString());
  }

  public static String removeOrder(String query) {
    return ORDER.matcher(query).replaceAll("");
  }
}
