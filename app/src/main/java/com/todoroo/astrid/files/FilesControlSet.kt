/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.data.TaskAttachment
import org.tasks.data.TaskAttachmentDao
import org.tasks.databinding.ControlSetFilesBinding
import org.tasks.dialogs.AddAttachmentDialog
import org.tasks.dialogs.DialogBuilder
import org.tasks.files.FileHelper
import org.tasks.preferences.Preferences
import org.tasks.ui.TaskEditControlFragment
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FilesControlSet : TaskEditControlFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var taskAttachmentDao: TaskAttachmentDao
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var preferences: Preferences
    
    private lateinit var attachmentContainer: LinearLayout
    private lateinit var addAttachment: TextView
    
    override fun createView(savedInstanceState: Bundle?) {
        val task = viewModel.task!!
        if (savedInstanceState == null) {
            if (task.hasTransitory(TaskAttachment.KEY)) {
                for (uri in (task.getTransitory<ArrayList<Uri>>(TaskAttachment.KEY))!!) {
                    newAttachment(uri)
                }
            }
        }

        lifecycleScope.launch {
            taskAttachmentDao
                    .getAttachments(task.uuid)
                    .forEach { addAttachment(it) }
        }
    }

    private fun addAttachment() {
        AddAttachmentDialog.newAddAttachmentDialog(this).show(parentFragmentManager, FRAG_TAG_ADD_ATTACHMENT_DIALOG)
    }

    override fun bind(parent: ViewGroup?) =
        ControlSetFilesBinding.inflate(layoutInflater, parent, true).let {
            attachmentContainer = it.attachmentContainer
            addAttachment = it.addAttachment.apply {
                setOnClickListener { addAttachment() }
            }
            it.root
        }

    override val icon = R.drawable.ic_outline_attachment_24px

    override fun controlId() = TAG

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AddAttachmentDialog.REQUEST_CAMERA || requestCode == AddAttachmentDialog.REQUEST_AUDIO) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data!!.data
                copyToAttachmentDirectory(uri)
                FileHelper.delete(activity, uri)
            }
        } else if (requestCode == AddAttachmentDialog.REQUEST_STORAGE || requestCode == AddAttachmentDialog.REQUEST_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                val clip = data!!.clipData
                if (clip != null) {
                    for (i in 0 until clip.itemCount) {
                        val item = clip.getItemAt(i)
                        copyToAttachmentDirectory(item.uri)
                    }
                } else {
                    copyToAttachmentDirectory(data.data)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun addAttachment(taskAttachment: TaskAttachment) {
        val fileRow = requireActivity().layoutInflater.inflate(R.layout.file_row, attachmentContainer, false)
        fileRow.tag = taskAttachment
        attachmentContainer.addView(fileRow)
        addAttachment(taskAttachment, fileRow)
    }

    private fun addAttachment(taskAttachment: TaskAttachment, fileRow: View) {
        val nameView = fileRow.findViewById<TextView>(R.id.file_text)
        val name = LEFT_TO_RIGHT_MARK.toString() + taskAttachment.name
        nameView.text = name
        nameView.setOnClickListener { showFile(taskAttachment) }
        val clearFile = fileRow.findViewById<View>(R.id.clear)
        clearFile.setOnClickListener {
            dialogBuilder
                    .newDialog(R.string.premium_remove_file_confirm)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        lifecycleScope.launch {
                            withContext(NonCancellable) {
                                taskAttachmentDao.delete(taskAttachment)
                                FileHelper.delete(context, taskAttachment.parseUri())
                            }
                            attachmentContainer.removeView(fileRow)
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

    @SuppressLint("NewApi")
    private fun showFile(m: TaskAttachment) {
        FileHelper.startActionView(requireActivity(), m.parseUri())
    }

    private fun copyToAttachmentDirectory(input: Uri?) {
        newAttachment(FileHelper.copyToUri(requireContext(), preferences.attachmentsDirectory!!, input!!))
    }

    private fun newAttachment(output: Uri) {
        val attachment = TaskAttachment(
                viewModel.task!!.uuid,
                output,
                FileHelper.getFilename(requireContext(), output)!!)
        lifecycleScope.launch {
            taskAttachmentDao.createNew(attachment)
            addAttachment(attachment)
        }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_files_pref
        private const val FRAG_TAG_ADD_ATTACHMENT_DIALOG = "frag_tag_add_attachment_dialog"
        private const val LEFT_TO_RIGHT_MARK = '\u200e'
    }
}