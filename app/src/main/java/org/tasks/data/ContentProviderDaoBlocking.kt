package org.tasks.data

import android.database.Cursor
import android.database.MatrixCursor
import androidx.sqlite.SQLiteStatement
import kotlinx.coroutines.runBlocking
import org.tasks.data.dao.Astrid2ContentProviderDao
import org.tasks.data.db.Database
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import javax.inject.Inject

class ContentProviderDaoBlocking @Inject constructor(
    private val dao: Astrid2ContentProviderDao,
    private val database: Database,
) {
    fun getTagNames(taskId: Long): List<String> = runBlocking {
        dao.getTagNames(taskId)
    }

    fun getAstrid2TaskProviderTasks(): List<Task> = runBlocking {
        dao.getAstrid2TaskProviderTasks()
    }

    fun tagDataOrderedByName(): List<TagData> = runBlocking {
        dao.tagDataOrderedByName()
    }

    fun getTasks(): Cursor = runBlocking { rawQuery("SELECT * FROM tasks") }

    fun getLists(): Cursor = runBlocking {
        rawQuery("""
            SELECT caldav_lists.*, caldav_accounts.cda_name
            FROM caldav_lists
            INNER JOIN caldav_accounts ON cdl_account = cda_uuid
            """.trimIndent()
        )
    }

    fun rawQuery(query: String): Cursor = runBlocking { database.rawQuery(query) { it.toCursor() } }
}

private fun SQLiteStatement.toCursor(): Cursor {
    val cursor = MatrixCursor(getColumnNames().toTypedArray())
    while (step()) {
        cursor.addRow((0 until getColumnCount()).map { getText(it) })
    }
    return cursor
}
