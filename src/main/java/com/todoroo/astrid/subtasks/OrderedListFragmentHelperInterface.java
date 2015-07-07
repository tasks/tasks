package com.todoroo.astrid.subtasks;

import android.content.Context;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Task;

import java.util.concurrent.atomic.AtomicReference;

public interface OrderedListFragmentHelperInterface<T> {

    void setUpUiComponents();
    void beforeSetUpTaskList(Filter filter);
    void onCreateTask(long id, String uuid);
    void onDeleteTask(Task task);
    TaskAdapter createTaskAdapter(Context context, TodorooCursor<Task> cursor, AtomicReference<String> queryTemplate);
    Property<?>[] taskProperties();

}
