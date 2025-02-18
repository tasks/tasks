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
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.Strings
import org.tasks.compose.edit.AttachmentRow
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.entity.TaskAttachment
import org.tasks.dialogs.AddAttachmentDialog
import org.tasks.extensions.Context.takePersistableUriPermission
import org.tasks.files.FileHelper
import org.tasks.preferences.Preferences
import org.tasks.ui.TaskEditControlFragment
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FilesControlSet : TaskEditControlFragment() {
    @Inject lateinit var taskAttachmentDao: TaskAttachmentDao
    @Inject lateinit var preferences: Preferences
    
    override fun createView(savedInstanceState: Bundle?) {
        val task = viewModel.viewState.value.task
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
                val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
                AttachmentRow(
                    attachments = viewState.attachments,
                    openAttachment = {
                        Timber.d("Clicked open $it")
                        FileHelper.startActionView(
                            context,
                            if (Strings.isNullOrEmpty(it.uri)) null else Uri.parse(it.uri)
                        )
                    },
                    deleteAttachment = {
                        Timber.d("Clicked delete $it")
                        viewModel.setAttachments(viewState.attachments - it)
                    },
                    addAttachment = {
                        Timber.d("Add attachment clicked")
                        AddAttachmentDialog.newAddAttachmentDialog(this@FilesControlSet)
                            .show(parentFragmentManager, FRAG_TAG_ADD_ATTACHMENT_DIALOG)
                    },
                )
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AddAttachmentDialog.REQUEST_CAMERA || requestCode == AddAttachmentDialog.REQUEST_AUDIO) {
            if (resultCode == Activity.RESULT_OK) {
                lifecycleScope.launch {
                    val uri = data!!.data
                    copyToAttachmentDirectory(uri)
                    FileHelper.delete(requireContext(), uri)
                }
            }
        } else if (requestCode == AddAttachmentDialog.REQUEST_STORAGE || requestCode == AddAttachmentDialog.REQUEST_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                lifecycleScope.launch {
                    val clip = data!!.clipData
                    if (clip != null) {
                        for (i in 0 until clip.itemCount) {
                            val item = clip.getItemAt(i)
                            requireContext().takePersistableUriPermission(item.uri)
                            copyToAttachmentDirectory(item.uri)
                        }
                    } else {
                        requireContext().takePersistableUriPermission(data.data!!)
                        copyToAttachmentDirectory(data.data)
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private suspend fun copyToAttachmentDirectory(input: Uri?) {
        val destination = preferences.attachmentsDirectory ?: return
        newAttachment(
            if (FileHelper.isInTree(requireContext(), destination, input!!)) {
                Timber.d("$input already exists in $destination")
                input
            } else {
                Timber.d("Copying $input to $destination")
                FileHelper.copyToUri(requireContext(), destination, input)
            }
        )
    }

    private fun newAttachment(output: Uri) {
        val attachment = TaskAttachment(
            uri = output.toString(),
            name = FileHelper.getFilename(requireContext(), output)!!,
        )
        lifecycleScope.launch {
            taskAttachmentDao.insert(attachment)
            viewModel.setAttachments(
                viewModel.viewState.value.attachments +
                        (taskAttachmentDao.getAttachment(attachment.remoteId) ?: return@launch))
        }
    }

    companion object {
        val TAG = R.string.TEA_ctrl_files_pref
        private const val FRAG_TAG_ADD_ATTACHMENT_DIALOG = "frag_tag_add_attachment_dialog"
    }
}
