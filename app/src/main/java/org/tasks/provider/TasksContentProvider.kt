package org.tasks.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import org.tasks.data.entity.Task
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.data.ContentProviderDaoBlocking

class TasksContentProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TasksContentProviderEntryPoint {
        val contentProviderDao: ContentProviderDaoBlocking
        val firebase: Firebase
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?): Cursor? {
        val hilt = hilt()
        return when (URI_MATCHER.match(uri)) {
            URI_TODO_AGENDA -> {
                hilt.firebase.logEvent(R.string.event_todoagenda)
                hilt.contentProviderDao.rawQuery(
                    SupportSQLiteQueryBuilder
                        .builder(TODO_AGENDA_TABLES)
                        .selection(selection, selectionArgs)
                        .create()
                        .sql
                )
            }
            URI_TASKS -> hilt.contentProviderDao.getTasks()
            URI_LISTS -> hilt.contentProviderDao.getLists()
            URI_GOOGLE_TASK_LISTS -> null
            else -> throw IllegalStateException("Unrecognized URI: $uri")
        }
    }

    override fun onCreate() = true

    override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?) = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String? = null

    private fun hilt() =
            EntryPointAccessors.fromApplication(
                    context!!.applicationContext,
                    TasksContentProviderEntryPoint::class.java)

    companion object {
        private const val TODO_AGENDA_TABLES =
                """${Task.TABLE_NAME}
                LEFT JOIN caldav_tasks ON cd_task = _id
                LEFT JOIN caldav_lists ON cdl_uuid = cd_calendar"""
        private const val AUTHORITY = BuildConfig.APPLICATION_ID
        private const val PURE_CALENDAR_WIDGET = "org.tasks.tasksprovider"
        @JvmField val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")
        const val URI_TASKS = 1
        const val URI_OPEN_TASK = 2
        private const val URI_LISTS = 3
        private const val URI_GOOGLE_TASK_LISTS = 4
        private const val URI_TODO_AGENDA = 100
        val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "tasks", URI_TASKS)
            addURI(AUTHORITY, "tasks/*", URI_OPEN_TASK)
            addURI(AUTHORITY, "lists", URI_LISTS)
            addURI(AUTHORITY, "google_lists", URI_GOOGLE_TASK_LISTS)
            addURI(AUTHORITY, "todoagenda", URI_TODO_AGENDA)
            addURI(PURE_CALENDAR_WIDGET, "tasks/*", URI_OPEN_TASK)
        }
    }
}