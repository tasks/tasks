package org.tasks.data

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteQuery
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class ContentProviderDaoBlocking @Inject constructor(private val dao: ContentProviderDao) {
    fun getTagNames(taskId: Long): List<String> = runBlocking {
        dao.getTagNames(taskId)
    }

    fun getAstrid2TaskProviderTasks(): List<Task> = runBlocking {
        dao.getAstrid2TaskProviderTasks()
    }

    fun tagDataOrderedByName(): List<TagData> = runBlocking {
        dao.tagDataOrderedByName()
    }

    fun getTasks(): Cursor = dao.getTasks()

    fun getLists(): Cursor = dao.getLists()

    fun getGoogleTaskLists(): Cursor = dao.getGoogleTaskLists()

    fun rawQuery(query: SupportSQLiteQuery): Cursor = dao.rawQuery(query)
}