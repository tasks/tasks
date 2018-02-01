package org.tasks.data;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.preferences.Preferences;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import static com.todoroo.astrid.activity.TaskListFragment.FILE_METADATA_JOIN;
import static com.todoroo.astrid.activity.TaskListFragment.TAGS_METADATA_JOIN;

public class TaskListDataProvider {

    private final TaskDao taskDao;
    private final Preferences preferences;

    @Inject
    public TaskListDataProvider(TaskDao taskDao, Preferences preferences) {
        this.taskDao = taskDao;
        this.preferences = preferences;
    }

    public List<Task> toList(Filter filter) {
        return toList(filter, Task.PROPERTIES);
    }

    public List<Task> toList(Filter filter, Property<?>[] properties) {
        Criterion tagsJoinCriterion = Criterion.and(
                Task.ID.eq(Field.field(TAGS_METADATA_JOIN + ".task")));

        if (filter instanceof TagFilter) {
            String uuid = ((TagFilter) filter).getUuid();
            tagsJoinCriterion = Criterion.and(tagsJoinCriterion, Field.field(TAGS_METADATA_JOIN + ".tag_uid").neq(uuid));
        }

        // TODO: For now, we'll modify the query to join and include the things like tag data here.
        // Eventually, we might consider restructuring things so that this query is constructed elsewhere.
        String joinedQuery =
                Join.left(Tag.TABLE.as(TAGS_METADATA_JOIN),
                        tagsJoinCriterion).toString() //$NON-NLS-1$
                        + Join.left(TaskAttachment.TABLE.as(FILE_METADATA_JOIN), Task.UUID.eq(Field.field(FILE_METADATA_JOIN + ".task_id")))
                        + filter.getSqlQuery();

        String query = SortHelper.adjustQueryForFlagsAndSort(
                preferences, joinedQuery, preferences.getSortMode());

        String groupedQuery;
        if (query.contains("GROUP BY")) {
            groupedQuery = query;
        } else if (query.contains("ORDER BY")) {
            groupedQuery = query.replace("ORDER BY", "GROUP BY " + Task.ID + " ORDER BY"); //$NON-NLS-1$
        } else {
            groupedQuery = query + " GROUP BY " + Task.ID;
        }

        return taskDao.fetchFiltered(groupedQuery, properties);
    }
}
