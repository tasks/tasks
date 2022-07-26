/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.edit.AttachmentRow
import org.tasks.data.TaskAttachment
import org.tasks.data.TaskAttachmentDao
import org.tasks.dialogs.AddAttachmentDialog
import org.tasks.dialogs.DialogBuilder
import org.tasks.files.FileHelper
import org.tasks.preferences.Preferences
import org.tasks.ui.TaskEditControlFragment
import javax.inject.Inject

@AndroidEntryPoint
class FilesControlSet : TaskEditControlFragment() {
    @Inject lateinit var taskAttachmentDao: TaskAttachmentDao
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var preferences: Preferences
    
    override fun createView(savedInstanceState: Bundle?) {
        val task = viewModel.task
        if (savedInstanceState == null) {
            if (task.hasTransitory(TaskAttachment.KEY)) {
                for (uri in (task.getTransitory<ArrayList<Uri>>(TaskAttachment.KEY))!!) {
                    newAttachment(uri)
                }
            }
        }
    }

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                MdcTheme {
                    AttachmentRow(
                        attachments = taskAttachmentDao.watchAttachments(viewModel.task.uuid)
                            .collectAsStateLifecycleAware(initial = emptyList()).value,
                        openAttachment = {
                            FileHelper.startActionView(requireActivity(), it.parseUri())
                        },
                        deleteAttachment = this@FilesControlSet::deleteAttachment,
                        addAttachment = {
                            AddAttachmentDialog.newAddAttachmentDialog(this@FilesControlSet)
                                .show(parentFragmentManager, FRAG_TAG_ADD_ATTACHMENT_DIALOG)
                        },
                    )
                }
            }
        }

    override fun controlId() = TAG

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AddAttachmentDialog.REQUEST_CAMERA || requestCode == AddAttachmentDialog.REQUEST_AUDIO) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data!!.data
                copyToAttachmentDirectory(uri)
                FileHelper.delete(requireContext(), uri)
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

    private fun deleteAttachment(attachment: TaskAttachment) {
        dialogBuilder
            .newDialog(R.string.premium_remove_file_confirm)
            .setPositiveButton(R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    withContext(NonCancellable) {
                        taskAttachmentDao.delete(attachment)
                        FileHelper.delete(context, attachment.parseUri())
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun copyToAttachmentDirectory(input: Uri?) {
        newAttachment(FileHelper.copyToUri(requireContext(), preferences.attachmentsDirectory!!, input!!))
    }

    private fun newAttachment(output: Uri) {
        val attachment = TaskAttachment(
                viewModel.task.uuid,
                output,
                FileHelper.getFilename(requireContext(), output)!!)
        lifecycleScope.launch {
            taskAttachmentDao.createNew(attachment)
        }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_files_pref
        private const val FRAG_TAG_ADD_ATTACHMENT_DIALOG = "frag_tag_add_attachment_dialog"
    }
}
