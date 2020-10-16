/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup

import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Handler
import android.text.TextUtils
import com.todoroo.andlib.utility.DialogUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskMover
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.backup.XmlReader
import org.tasks.data.*
import org.tasks.data.Place.Companion.newPlace
import org.tasks.dialogs.DialogBuilder
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject

class TasksXmlImporter @Inject constructor(
        private val tagDataDao: TagDataDao,
        private val userActivityDao: UserActivityDao,
        private val dialogBuilder: DialogBuilder,
        private val taskDao: TaskDao,
        private val locationDao: LocationDao,
        private val localBroadcastManager: LocalBroadcastManager,
        private val alarmDao: AlarmDao,
        private val tagDao: TagDao,
        private val googleTaskDao: GoogleTaskDao,
        private val taskMover: TaskMover,
        private val firebase: Firebase) {

    private var activity: Activity? = null
    private var handler: Handler? = null
    private var taskCount = 0
    private var importCount = 0
    private var skipCount = 0
    private var errorCount = 0
    private var progressDialog: ProgressDialog? = null
    private var input: Uri? = null

    private fun setProgressMessage(message: String) {
        handler!!.post { progressDialog!!.setMessage(message) }
    }

    suspend fun importTasks(activity: Activity?, input: Uri?, progressDialog: ProgressDialog?) {
        this.activity = activity
        this.input = input
        this.progressDialog = progressDialog
        try {
            performImport()
            taskMover.migrateLocalTasks()
            firebase.logEvent(R.string.event_xml_import)
        } catch (e: IOException) {
            firebase.reportException(e)
        } catch (e: XmlPullParserException) {
            firebase.reportException(e)
        }
    }

    // --- importers
    // =============================================================== FORMAT2
    @Throws(IOException::class, XmlPullParserException::class)
    private suspend fun performImport() {
        val factory = XmlPullParserFactory.newInstance()
        val xpp = factory.newPullParser()
        val inputStream = activity!!.contentResolver.openInputStream(input!!)
        val reader = InputStreamReader(inputStream)
        xpp.setInput(reader)
        try {
            while (xpp.next() != XmlPullParser.END_DOCUMENT) {
                val tag = xpp.name
                if (xpp.eventType == XmlPullParser.END_TAG) {
                    // Ignore end tags
                    continue
                }
                if (tag != null) {
                    // Process <astrid ... >
                    if (tag == BackupConstants.ASTRID_TAG) {
                        val format = xpp.getAttributeValue(null, BackupConstants.ASTRID_ATTR_FORMAT)
                        when {
                            TextUtils.equals(format, FORMAT2) -> Format2TaskImporter(xpp).process()
                            TextUtils.equals(format, FORMAT3) -> Format3TaskImporter(xpp).process()
                            else -> throw UnsupportedOperationException(
                                        "Did not know how to import tasks with xml format '$format'")
                        }
                    }
                }
            }
        } finally {
            reader.close()
            inputStream!!.close()
            localBroadcastManager.broadcastRefresh()
            handler!!.post {
                if (progressDialog!!.isShowing) {
                    DialogUtilities.dismissDialog(activity, progressDialog)
                    showSummary()
                }
            }
        }
    }

    private fun showSummary() {
        val r = activity!!.resources
        dialogBuilder
                .newDialog(R.string.import_summary_title)
                .setMessage(
                        activity!!.getString(
                                R.string.import_summary_message,
                                "",
                                r.getQuantityString(R.plurals.Ntasks, taskCount, taskCount),
                                r.getQuantityString(R.plurals.Ntasks, importCount, importCount),
                                r.getQuantityString(R.plurals.Ntasks, skipCount, skipCount),
                                r.getQuantityString(R.plurals.Ntasks, errorCount, errorCount)))
                .setPositiveButton(R.string.ok) { dialog: DialogInterface, id: Int -> dialog.dismiss() }
                .show()
    }

    // =============================================================== FORMAT3
    private open inner class Format2TaskImporter {
        var xpp: XmlPullParser? = null
        var currentTask: Task? = null

        internal constructor()

        internal constructor(xpp: XmlPullParser) {
            this.xpp = xpp
        }

        open suspend fun process() {
            while (xpp?.next() != XmlPullParser.END_DOCUMENT) {
                val tag = xpp?.name
                if (tag == null || xpp?.eventType == XmlPullParser.END_TAG) {
                    continue
                }
                try {
                    when (tag) {
                        BackupConstants.TASK_TAG -> parseTask()
                        BackupConstants.COMMENT_TAG -> parseComment()
                        BackupConstants.METADATA_TAG -> parseMetadata(2)
                    }
                } catch (e: Exception) {
                    errorCount++
                    Timber.e(e)
                }
            }
        }

        suspend fun parseTask() {
            taskCount++
            setProgressMessage(activity!!.getString(R.string.import_progress_read, taskCount))
            currentTask = Task(XmlReader(xpp))
            val existingTask = taskDao.fetch(currentTask!!.uuid)
            if (existingTask == null) {
                taskDao.createNew(currentTask!!)
                importCount++
            } else {
                skipCount++
            }
        }

        /** Imports a comment from the XML we're reading. taken from EditNoteActivity.addComment()  */
        suspend fun parseComment() {
            if (!currentTask!!.isSaved) {
                return
            }
            val userActivity = UserActivity(XmlReader(xpp))
            userActivityDao.createNew(userActivity)
        }

        suspend fun parseMetadata(format: Int) {
            if (!currentTask!!.isSaved) {
                return
            }
            val xml = XmlReader(xpp)
            val key = xml.readString("key")
            if ("alarm" == key) {
                val alarm = Alarm()
                alarm.task = currentTask!!.id
                alarm.time = xml.readLong("value")
                alarmDao.insert(alarm)
            } else if ("geofence" == key) {
                val place = newPlace()
                place.name = xml.readString("value")
                place.latitude = xml.readDouble("value2")
                place.longitude = xml.readDouble("value3")
                locationDao.insert(place)
                val geofence = Geofence()
                geofence.task = currentTask!!.id
                geofence.place = place.uid
                geofence.radius = xml.readInteger("value4")
                geofence.isArrival = true
                locationDao.insert(geofence)
            } else if ("tags-tag" == key) {
                val name = xml.readString("value")
                val tagUid = xml.readString("value2")
                if (tagDao.getTagByTaskAndTagUid(currentTask!!.id, tagUid) == null) {
                    tagDao.insert(Tag(currentTask!!, name, tagUid))
                }
                // Construct the TagData from Metadata
                // Fix for failed backup, Version before 4.6.10
                if (format == 2) {
                    var tagData = tagDataDao.getByUuid(tagUid)
                    if (tagData == null) {
                        tagData = TagData()
                        tagData.remoteId = tagUid
                        tagData.name = name
                        tagDataDao.createNew(tagData)
                    }
                }
            } else if ("gtasks" == key) {
                val googleTask = GoogleTask()
                googleTask.task = currentTask!!.id
                googleTask.remoteId = xml.readString("value")
                googleTask.listId = xml.readString("value2")
                googleTask.parent = xml.readLong("value3")
                googleTask.order = xml.readLong("value5")
                googleTask.remoteOrder = xml.readLong("value6")
                googleTask.lastSync = xml.readLong("value7")
                googleTask.deleted = xml.readLong("deleted")
                googleTaskDao.insert(googleTask)
            }
        }
    }

    private inner class Format3TaskImporter internal constructor(xpp: XmlPullParser) : Format2TaskImporter() {
        private suspend fun parseTagdata() {
            val tagData = TagData(XmlReader(xpp))
            if (tagDataDao.getByUuid(tagData.remoteId!!) == null) {
                tagDataDao.createNew(tagData)
            }
        }

        init {
            this.xpp = xpp
        }

        override suspend fun process() {
            while (xpp?.next() != XmlPullParser.END_DOCUMENT) {
                val tag = xpp?.name
                if (tag == null || xpp?.eventType == XmlPullParser.END_TAG) {
                    continue
                }
                try {
                    when (tag) {
                        BackupConstants.TASK_TAG -> parseTask()
                        BackupConstants.METADATA_TAG -> parseMetadata(3)
                        BackupConstants.COMMENT_TAG -> parseComment()
                        BackupConstants.TAGDATA_TAG -> parseTagdata()
                    }
                } catch (e: Exception) {
                    errorCount++
                    Timber.e(e)
                }
            }
        }
    }

    companion object {
        private const val FORMAT2 = "2" // $NON-NLS-1$
        private const val FORMAT3 = "3" // $NON-NLS-1$
    }
}