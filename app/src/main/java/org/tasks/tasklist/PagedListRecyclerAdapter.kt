package org.tasks.tasklist

import androidx.paging.AsyncPagedListDiffer
import androidx.paging.PagedList
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.adapter.TaskAdapter
import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.TaskContainer
import org.tasks.preferences.Preferences

class PagedListRecyclerAdapter(
        adapter: TaskAdapter,
        private val recyclerView: RecyclerView,
        viewHolderFactory: ViewHolderFactory,
        taskList: TaskListFragment,
        list: List<TaskContainer>,
        taskDao: TaskDao,
        preferences: Preferences) : TaskListRecyclerAdapter(adapter, viewHolderFactory, taskList, taskDao, preferences) {

    private val differ: AsyncPagedListDiffer<TaskContainer> =
            AsyncPagedListDiffer(this, AsyncDifferConfig.Builder(ItemCallback()).build())

    override fun getItem(position: Int) = differ.getItem(position)

    override fun submitList(list: List<TaskContainer>) = differ.submitList(list as PagedList<TaskContainer>)

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        val recyclerViewState = recyclerView.layoutManager!!.onSaveInstanceState()
        super.onMoved(fromPosition, toPosition)
        recyclerView.layoutManager!!.onRestoreInstanceState(recyclerViewState)
    }

    override fun dragAndDropEnabled() = false

    override fun getItemCount() = differ.itemCount

    override fun getTaskCount() = itemCount

    init {
        if (list is PagedList<*>) {
            differ.submitList(list as PagedList<TaskContainer>?)
        }
    }
}