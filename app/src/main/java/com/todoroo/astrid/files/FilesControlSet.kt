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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.Strings
import org.tasks.compose.edit.AttachmentRow
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.entity.TaskAttachment
import org.tasks.dialogs.AddAttachmentDialog
import org.tasks.files.FileHelper
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import org.tasks.ui.TaskEditControlFragment
import javax.inject.Inject

@AndroidEntryPoint
class FilesControlSet : TaskEditControlFragment() {
    @Inject lateinit var taskAttachmentDao: TaskAttachmentDao
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
                TasksTheme {
                    AttachmentRow(
                        attachments = viewModel.selectedAttachments.collectAsStateWithLifecycle().value,
                        openAttachment = {
                            FileHelper.startActionView(
                                requireActivity(),
                                if (Strings.isNullOrEmpty(it.uri)) null else Uri.parse(it.uri)
                            )
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
        viewModel.selectedAttachments.update {
            it.minus(attachment)
        }
    }

    private fun copyToAttachmentDirectory(input: Uri?) {
        newAttachment(FileHelper.copyToUri(requireContext(), preferences.attachmentsDirectory!!, input!!))
    }

    private fun newAttachment(output: Uri) {
        val attachment = TaskAttachment(
            uri = output.toString(),
            name = FileHelper.getFilename(requireContext(), output)!!,
        )
        lifecycleScope.launch {
            taskAttachmentDao.insert(attachment)
            viewModel.selectedAttachments.update {
                it.plus(
                    taskAttachmentDao.getAttachment(attachment.remoteId) ?: return@launch
                )
            }
        }
    }

    companion object {
        val TAG = R.string.TEA_ctrl_files_pref
        private const val FRAG_TAG_ADD_ATTACHMENT_DIALOG = "frag_tag_add_attachment_dialog"
    }
}
