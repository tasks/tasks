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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.data.TaskAttachment
import org.tasks.data.TaskAttachmentDao
import org.tasks.dialogs.AddAttachmentDialog
import org.tasks.dialogs.DialogBuilder
import org.tasks.files.FileHelper
import org.tasks.preferences.Preferences
import org.tasks.ui.TaskEditControlComposeFragment
import javax.inject.Inject

@AndroidEntryPoint
class FilesControlSet : TaskEditControlComposeFragment() {
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

    private fun addAttachment() {
        AddAttachmentDialog.newAddAttachmentDialog(this)
            .show(parentFragmentManager, FRAG_TAG_ADD_ATTACHMENT_DIALOG)
    }

    @Composable
    override fun Body() {
        val attachments =
            taskAttachmentDao.watchAttachments(viewModel.task.uuid)
                .collectAsStateLifecycleAware(initial = emptyList()).value
        Column(
            modifier = Modifier.padding(top = if (attachments.isEmpty()) 0.dp else 8.dp),
        ) {
            attachments.forEach {
                Row(
                    modifier = Modifier
                        .clickable { showFile(it) },
                    verticalAlignment = CenterVertically,
                ) {
                    Text(
                        text = it.name!!,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { deleteAttachment(it) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(
                                id = R.string.delete
                            )
                        )
                    }
                }
            }
            DisabledText(
                text = stringResource(id = R.string.add_attachment),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { addAttachment() }
                    .padding(
                        top = if (attachments.isEmpty()) 20.dp else 8.dp,
                        bottom = 20.dp,
                    )
            )
        }
    }

    override val icon = R.drawable.ic_outline_attachment_24px

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

    @SuppressLint("NewApi")
    private fun showFile(m: TaskAttachment) {
        FileHelper.startActionView(requireActivity(), m.parseUri())
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