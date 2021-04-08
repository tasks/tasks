/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.net.Uri
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.data.UserActivity
import org.tasks.data.UserActivityDao
import org.tasks.dialogs.Linkify
import org.tasks.files.FileHelper
import org.tasks.files.ImageHelper
import org.tasks.locale.Locale
import org.tasks.preferences.Preferences
import java.util.*
import javax.inject.Inject
import kotlin.math.min

class CommentsController @Inject constructor(
        private val userActivityDao: UserActivityDao,
        private val activity: Activity,
        private val preferences: Preferences,
        private val locale: Locale
) {
    private val items = ArrayList<UserActivity>()
    private var commentItems = 10
    private var task: Task? = null
    private var commentsContainer: ViewGroup? = null

    fun initialize(task: Task?, commentsContainer: ViewGroup?) {
        this.task = task
        this.commentsContainer = commentsContainer
    }

    fun reloadView() {
        if (!preferences.getBoolean(R.string.p_show_task_edit_comments, true)) {
            return
        }
        (activity as AppCompatActivity).lifecycleScope.launch {
            items.clear()
            commentsContainer!!.removeAllViews()
            items.addAll(userActivityDao.getCommentsForTask(task!!.uuid))
            for (i in 0 until min(items.size, commentItems)) {
                val notesView = getUpdateNotes(items[i], commentsContainer)
                commentsContainer!!.addView(notesView)
            }
            if (items.size > commentItems) {
                val loadMore = Button(activity)
                loadMore.setHint(R.string.TEA_load_more)
                loadMore.setBackgroundColor(Color.alpha(0))
                loadMore.setOnClickListener {
                    // Perform action on click
                    commentItems += 10
                    reloadView()
                }
                commentsContainer!!.addView(loadMore)
            }
        }
    }

    private fun getUpdateNotes(userActivity: UserActivity, parent: ViewGroup?): View {
        val convertView = activity.layoutInflater.inflate(R.layout.comment_adapter_row, parent, false)
        bindView(convertView, userActivity)
        return convertView
    }

    /** Helper method to set the contents and visibility of each field  */
    private fun bindView(view: View, item: UserActivity) {
        // name
        val nameView = view.findViewById<TextView>(R.id.title)
        nameView.text = Html.fromHtml(item.message)
        Linkify.safeLinkify(nameView)

        // date
        val date = view.findViewById<TextView>(R.id.date)
        date.text = DateUtilities.getLongDateStringWithTime(item.created!!, locale.locale)

        // picture
        val commentPictureView = view.findViewById<ImageView>(R.id.comment_picture)
        setupImagePopupForCommentView(view, commentPictureView, item.pictureUri, activity)

        // delete button
        val deleteBtn = view.findViewById<ImageView>(R.id.clear)
        deleteBtn.setOnClickListener(){
            val builder = AlertDialog.Builder(commentsContainer!!.context)

            // Display a message on alert dialog
            builder.setMessage(R.string.delete_comment)

            // Set a positive button and its click listener on alert dialog
            builder.setPositiveButton(R.string.delete){dialog, which ->

                (activity as AppCompatActivity).lifecycleScope.launch {
                    withContext(NonCancellable) {
                        userActivityDao.delete(item)
                    }
                    reloadView()
                }
            }

            // Display a negative button on alert dialog
            builder.setNegativeButton(R.string.cancel){dialog,which ->
            }
            // Finally, make the alert dialog using builder
            val dialog: AlertDialog = builder.create()

            // Display the alert dialog on app interface
            dialog.show()
        }
    }

    companion object {
        private fun setupImagePopupForCommentView(
                view: View, commentPictureView: ImageView, uri: Uri?, activity: Activity) {
            if (uri != null) {
                commentPictureView.visibility = View.VISIBLE
                commentPictureView.setImageBitmap(
                        ImageHelper.sampleBitmap(
                                activity,
                                uri,
                                commentPictureView.layoutParams.width,
                                commentPictureView.layoutParams.height))
                view.setOnClickListener { FileHelper.startActionView(activity, uri) }
            } else {
                commentPictureView.visibility = View.GONE
            }
        }
    }
}