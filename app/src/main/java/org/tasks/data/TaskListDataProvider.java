package org.tasks.data;

import android.database.sqlite.SQLiteException;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.preferences.Preferences;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import timber.log.Timber;

import static com.todoroo.astrid.activity.TaskListFragment.FILE_METADATA_JOIN;
import static com.todoroo.astrid.activity.TaskListFragment.TAGS_METADATA_JOIN;

public class TaskListDataProvider {

    private final AtomicReference<String> sqlQueryTemplate = new AtomicReference<>();
    private final TaskDao taskDao;
    private final Preferences preferences;

    @Inject
    public TaskListDataProvider(TaskDao taskDao, Preferences preferences) {
        this.taskDao = taskDao;
        this.preferences = preferences;
    }

    public TodorooCursor<Task> constructCursor(Filter filter, Property<?>[] properties) {
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

        sqlQueryTemplate.set(SortHelper.adjustQueryForFlagsAndSort(
                preferences, joinedQuery, preferences.getSortMode()));

        String groupedQuery;
        if (sqlQueryTemplate.get().contains("GROUP BY")) {
            groupedQuery = sqlQueryTemplate.get();
        } else if (sqlQueryTemplate.get().contains("ORDER BY")) {
            groupedQuery = sqlQueryTemplate.get().replace("ORDER BY", "GROUP BY " + Task.ID + " ORDER BY"); //$NON-NLS-1$
        } else {
            groupedQuery = sqlQueryTemplate.get() + " GROUP BY " + Task.ID;
        }
        sqlQueryTemplate.set(groupedQuery);

        // Peform query
        try {
            return taskDao.fetchFiltered(sqlQueryTemplate.get(), properties);
        } catch (SQLiteException e) {
            // We don't show this error anymore--seems like this can get triggered
            // by a strange bug, but there seems to not be any negative side effect.
            // For now, we'll suppress the error
            // See http://astrid.com/home#tags-7tsoi/task-1119pk
            Timber.e(e, e.getMessage());
            return null;
        }
    }
}
