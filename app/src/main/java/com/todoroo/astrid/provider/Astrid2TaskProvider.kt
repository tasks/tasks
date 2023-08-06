/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.annotation.ColorRes
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.data.ContentProviderDaoBlocking
import timber.log.Timber
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * This is the legacy Astrid task provider. While it will continue to be supported, note that it
 * does not expose all of the information in Astrid, nor does it support many editing operations.
 *
 *
 * See the individual methods for a description of what is returned.
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
class Astrid2TaskProvider : ContentProvider() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Astrid2TaskProviderEntryPoint {
        val contentProviderDao: ContentProviderDaoBlocking
        val firebase: Firebase
    }

    companion object {
        private const val AUTHORITY = BuildConfig.APPLICATION_ID + ".tasksprovider"
        @JvmField val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")
        private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH)
        private const val NAME = "name"
        private const val IMPORTANCE_COLOR = "importance_color"
        private const val IDENTIFIER = "identifier"
        private const val PREFERRED_DUE_DATE = "preferredDueDate"
        private const val DEFINITE_DUE_DATE = "definiteDueDate"
        private const val IMPORTANCE = "importance"
        private const val ID = "id"
        private const val TAGS_ID = "tags_id"
        private val TASK_FIELD_LIST = arrayOf(
                NAME,
                IMPORTANCE_COLOR,
                PREFERRED_DUE_DATE,
                DEFINITE_DUE_DATE,
                IMPORTANCE,
                IDENTIFIER,
                TAGS_ID
        )
        private val TAGS_FIELD_LIST = arrayOf(ID, NAME)
        private const val URI_TASKS = 0
        private const val URI_TAGS = 1
        private const val TAG_SEPARATOR = "|"

        private fun getPriorityColor(context: Context?, priority: Int): Int {
            return context!!.getColor(getPriorityResId(priority))
        }

        @ColorRes
        private fun getPriorityResId(priority: Int): Int {
            return when {
                priority <= 0 -> R.color.red_500
                priority == 1 -> R.color.amber_500
                priority == 2 -> R.color.blue_500
                else -> R.color.grey_500
            }
        }

        init {
            URI_MATCHER.addURI(AUTHORITY, "tasks", URI_TASKS)
            URI_MATCHER.addURI(AUTHORITY, "tags", URI_TAGS)
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun onCreate() = true

    private fun hilt(): Astrid2TaskProviderEntryPoint {
        return EntryPointAccessors.fromApplication(
                context!!.applicationContext, Astrid2TaskProviderEntryPoint::class.java)
    }

    /**
     * Note: tag id is no longer a real column, so we pass in a UID generated from the tag string.
     *
     * @return two-column cursor: tag id (string) and tag name
     */
    private val tags: Cursor
        get() {
            val tags = hilt().contentProviderDao.tagDataOrderedByName()
            val ret = MatrixCursor(TAGS_FIELD_LIST)
            for (tag in tags) {
                val values = arrayOfNulls<Any>(2)
                values[0] = tagNameToLong(tag.name)
                values[1] = tag.name
                ret.addRow(values)
            }
            return ret
        }

    private fun tagNameToLong(tag: String?): Long {
        val m: MessageDigest = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            Timber.e(e)
            return -1
        }
        m.update(tag!!.toByteArray(), 0, tag.length)
        return BigInteger(1, m.digest()).toLong()
    }

    /**
     * Cursor with the following columns
     *
     *
     *  1. task title, string
     *  1. task importance color, int android RGB color
     *  1. task due date (was: preferred due date), long millis since epoch
     *  1. task due date (was: absolute due date), long millis since epoch
     *  1. task importance, integer from 0 to 3 (0 => most important)
     *  1. task id, long
     *  1. task tags, string tags separated by |
     *
     *
     * @return cursor as described above
     */
    private val tasks: Cursor
        get() {
            val hilt = hilt()
            hilt.firebase.logEvent(R.string.event_astrid2taskprovider)
            val tasks = hilt.contentProviderDao.getAstrid2TaskProviderTasks()
            val ret = MatrixCursor(TASK_FIELD_LIST)
            for (task in tasks) {
                val taskTags = getTagsAsString(task.id, TAG_SEPARATOR)
                val values = arrayOfNulls<Any>(7)
                values[0] = task.title
                values[1] = getPriorityColor(context, task.priority)
                values[2] = task.dueDate
                values[3] = task.dueDate
                values[4] = task.priority
                values[5] = task.id
                values[6] = taskTags
                ret.addRow(values)
            }
            return ret
        }

    override fun query(
            uri: Uri,
            projection: Array<String>?,
            selection: String?,
            selectionArgs: Array<String>?,
            sortOrder: String?
    ): Cursor {
        return when (URI_MATCHER.match(uri)) {
            URI_TASKS -> tasks
            URI_TAGS -> tags
            else -> throw IllegalStateException("Unrecognized URI:$uri")
        }
    }

    override fun update(
            uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("not supported")
    }

    /**
     * Return tags as a list of strings separated by given separator
     *
     * @return empty string if no tags, otherwise string
     */
    private fun getTagsAsString(taskId: Long, separator: String) =
            hilt().contentProviderDao.getTagNames(taskId).joinToString(separator)
}