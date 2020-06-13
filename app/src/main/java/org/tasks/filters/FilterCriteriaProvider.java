package org.tasks.filters;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newLinkedHashSet;

import android.content.Context;
import android.content.res.Resources;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.api.TextInputCriterion;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Task.Priority;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.Tag;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.injection.ApplicationContext;

public class FilterCriteriaProvider {

  private static final String IDENTIFIER_UNIVERSE = "active";
  private static final String IDENTIFIER_TITLE = "title";
  private static final String IDENTIFIER_IMPORTANCE = "importance";
  private static final String IDENTIFIER_DUEDATE = "dueDate";
  private static final String IDENTIFIER_GTASKS = "gtaskslist";
  private static final String IDENTIFIER_CALDAV = "caldavlist";
  private static final String IDENTIFIER_TAG_IS = "tag_is";
  private static final String IDENTIFIER_TAG_CONTAINS = "tag_contains";

  private final Context context;
  private final TagDataDao tagDataDao;
  private final Resources r;
  private final GoogleTaskListDao googleTaskListDao;
  private final CaldavDao caldavDao;

  @Inject
  public FilterCriteriaProvider(
      @ApplicationContext Context context,
      TagDataDao tagDataDao,
      GoogleTaskListDao googleTaskListDao,
      CaldavDao caldavDao) {
    this.context = context;
    this.tagDataDao = tagDataDao;

    r = context.getResources();
    this.googleTaskListDao = googleTaskListDao;
    this.caldavDao = caldavDao;
  }

  public CustomFilterCriterion getFilterCriteria(String identifier) {
    switch (identifier) {
      case IDENTIFIER_UNIVERSE:
        return getStartingUniverse();
      case IDENTIFIER_TITLE:
        return getTaskTitleContainsFilter();
      case IDENTIFIER_IMPORTANCE:
        return getPriorityFilter();
      case IDENTIFIER_DUEDATE:
        return getDueDateFilter();
      case IDENTIFIER_GTASKS:
        return getGtasksFilterCriteria();
      case IDENTIFIER_CALDAV:
        return getCaldavFilterCriteria();
      case IDENTIFIER_TAG_IS:
        return getTagFilter();
      case IDENTIFIER_TAG_CONTAINS:
        return getTagNameContainsFilter();
      default:
        throw new RuntimeException("Unknown identifier: " + identifier);
    }
  }

  public CustomFilterCriterion getStartingUniverse() {
    return new MultipleSelectCriterion(
        IDENTIFIER_UNIVERSE,
        context.getString(R.string.BFE_Active),
        null,
        null,
        null,
        null,
        null);
  }

  public List<CustomFilterCriterion> getAll() {
    List<CustomFilterCriterion> result = new ArrayList<>();

    result.add(getTagFilter());
    result.add(getTagNameContainsFilter());
    result.add(getDueDateFilter());
    result.add(getPriorityFilter());
    result.add(getTaskTitleContainsFilter());
    if (!googleTaskListDao.getAccounts().isEmpty()) {
      result.add(getGtasksFilterCriteria());
    }
    result.add(getCaldavFilterCriteria());
    return result;
  }

  private CustomFilterCriterion getTagFilter() {
    // TODO: adding to hash set because duplicate tag name bug hasn't been fixed yet
    String[] tagNames =
        newLinkedHashSet(transform(tagDataDao.tagDataOrderedByName(), TagData::getName))
            .toArray(new String[0]);
    Map<String, Object> values = new HashMap<>();
    values.put(Tag.KEY, "?");
    return new MultipleSelectCriterion(
        IDENTIFIER_TAG_IS,
        context.getString(R.string.CFC_tag_text),
        Query.select(Tag.TASK)
            .from(Tag.TABLE)
            .join(Join.inner(Task.TABLE, Tag.TASK.eq(Task.ID)))
            .where(Criterion.and(TaskDao.TaskCriteria.activeAndVisible(), Tag.NAME.eq("?")))
            .toString(),
        values,
        tagNames,
        tagNames,
        context.getString(R.string.CFC_tag_name));
  }

  public CustomFilterCriterion getTagNameContainsFilter() {
    return new TextInputCriterion(
        IDENTIFIER_TAG_CONTAINS,
        context.getString(R.string.CFC_tag_contains_text),
        Query.select(Tag.TASK)
            .from(Tag.TABLE)
            .join(Join.inner(Task.TABLE, Tag.TASK.eq(Task.ID)))
            .where(Criterion.and(TaskDao.TaskCriteria.activeAndVisible(), Tag.NAME.like("%?%")))
            .toString(),
        context.getString(R.string.CFC_tag_contains_name),
        "",
        context.getString(R.string.CFC_tag_contains_name));
  }

  public CustomFilterCriterion getDueDateFilter() {
    String[] entryValues =
        new String[] {
          "0",
          PermaSql.VALUE_EOD_YESTERDAY,
          PermaSql.VALUE_EOD,
          PermaSql.VALUE_EOD_TOMORROW,
          PermaSql.VALUE_EOD_DAY_AFTER,
          PermaSql.VALUE_EOD_NEXT_WEEK,
          PermaSql.VALUE_EOD_NEXT_MONTH,
        };
    Map<String, Object> values = new HashMap<>();
    values.put(Task.DUE_DATE.name, "?");
    return new MultipleSelectCriterion(
        IDENTIFIER_DUEDATE,
        r.getString(R.string.CFC_dueBefore_text),
        Query.select(Task.ID)
            .from(Task.TABLE)
            .where(
                Criterion.and(
                    TaskDao.TaskCriteria.activeAndVisible(),
                    Criterion.or(Field.field("?").eq(0), Task.DUE_DATE.gt(0)),
                    Task.DUE_DATE.lte("?")))
            .toString(),
        values,
        r.getStringArray(R.array.CFC_dueBefore_entries),
        entryValues,
        r.getString(R.string.CFC_dueBefore_name));
  }

  public CustomFilterCriterion getPriorityFilter() {
    String[] entryValues =
        new String[] {
          Integer.toString(Priority.HIGH),
          Integer.toString(Priority.MEDIUM),
          Integer.toString(Priority.LOW),
          Integer.toString(Priority.NONE),
        };
    String[] entries = new String[] {"!!!", "!!", "!", "o"};
    Map<String, Object> values = new HashMap<>();
    values.put(Task.IMPORTANCE.name, "?");
    return new MultipleSelectCriterion(
        IDENTIFIER_IMPORTANCE,
        r.getString(R.string.CFC_importance_text),
        Query.select(Task.ID)
            .from(Task.TABLE)
            .where(Criterion.and(TaskDao.TaskCriteria.activeAndVisible(), Task.IMPORTANCE.lte("?")))
            .toString(),
        values,
        entries,
        entryValues,
        r.getString(R.string.CFC_importance_name));
  }

  private CustomFilterCriterion getTaskTitleContainsFilter() {
    return new TextInputCriterion(
        IDENTIFIER_TITLE,
        r.getString(R.string.CFC_title_contains_text),
        Query.select(Task.ID)
            .from(Task.TABLE)
            .where(Criterion.and(TaskDao.TaskCriteria.activeAndVisible(), Task.TITLE.like("%?%")))
            .toString(),
        r.getString(R.string.CFC_title_contains_name),
        "",
        r.getString(R.string.CFC_title_contains_name));
  }

  private CustomFilterCriterion getGtasksFilterCriteria() {
    List<GoogleTaskList> lists = googleTaskListDao.getAllLists();

    String[] listNames = new String[lists.size()];
    String[] listIds = new String[lists.size()];
    for (int i = 0; i < lists.size(); i++) {
      listNames[i] = lists.get(i).getTitle();
      listIds[i] = lists.get(i).getRemoteId();
    }

    Map<String, Object> values = new HashMap<>();
    values.put(GoogleTask.KEY, "?");

    return new MultipleSelectCriterion(
        IDENTIFIER_GTASKS,
        context.getString(R.string.CFC_gtasks_list_text),
        Query.select(GoogleTask.TASK)
            .from(GoogleTask.TABLE)
            .join(Join.inner(Task.TABLE, GoogleTask.TASK.eq(Task.ID)))
            .where(
                Criterion.and(
                    TaskDao.TaskCriteria.activeAndVisible(),
                    GoogleTask.DELETED.eq(0),
                    GoogleTask.LIST.eq("?")))
            .toString(),
        values,
        listNames,
        listIds,
        context.getString(R.string.CFC_gtasks_list_name));
  }

  private CustomFilterCriterion getCaldavFilterCriteria() {
    List<CaldavCalendar> calendars = caldavDao.getCalendars();

    String[] names = new String[calendars.size()];
    String[] ids = new String[calendars.size()];
    for (int i = 0; i < calendars.size(); i++) {
      names[i] = calendars.get(i).getName();
      ids[i] = calendars.get(i).getUuid();
    }
    Map<String, Object> values = new HashMap<>();
    values.put(CaldavTask.KEY, "?");

    return new MultipleSelectCriterion(
        IDENTIFIER_CALDAV,
        context.getString(R.string.CFC_gtasks_list_text),
        Query.select(CaldavTask.TASK)
            .from(CaldavTask.TABLE)
            .join(Join.inner(Task.TABLE, CaldavTask.TASK.eq(Task.ID)))
            .where(
                Criterion.and(
                    TaskDao.TaskCriteria.activeAndVisible(),
                    CaldavTask.DELETED.eq(0),
                    CaldavTask.CALENDAR.eq("?")))
            .toString(),
        values,
        names,
        ids,
        context.getString(R.string.CFC_list_name));
  }
}
