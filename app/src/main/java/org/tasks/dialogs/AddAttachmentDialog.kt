package org.tasks.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.DialogFragment
import com.todoroo.astrid.files.FilesControlSet
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.activities.CameraActivity
import org.tasks.extensions.Fragment.safeStartActivityForResult
import org.tasks.files.FileHelper.newFilePickerIntent
import org.tasks.preferences.Device
import javax.inject.Inject

@AndroidEntryPoint
class AddAttachmentDialog : DialogFragment() {
    @Inject lateinit var context: Activity
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var device: Device

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val entries: MutableList<String> = ArrayList()
        val actions: MutableList<Runnable> = ArrayList()
        if (device.hasCamera()) {
            entries.add(getString(R.string.take_a_picture))
            actions.add { takePicture() }
        }
        if (device.hasMicrophone()) {
            entries.add(getString(R.string.premium_record_audio))
            actions.add { recordNote() }
        }
        entries.add(getString(R.string.pick_from_gallery))
        actions.add { pickFromGallery() }
        entries.add(getString(R.string.pick_from_storage))
        actions.add { pickFromStorage() }
        return dialogBuilder
                .newDialog()
                .setItems(entries) { _, which -> actions[which].run() }
                .show()
    }

    private fun takePicture() {
        targetFragment?.startActivityForResult(
                Intent(context, CameraActivity::class.java),
                REQUEST_CAMERA
        )
    }

    private fun recordNote() {
        targetFragment?.safeStartActivityForResult(
            Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION),
            REQUEST_AUDIO
        )
    }

    private fun pickFromGallery() {
        targetFragment?.safeStartActivityForResult(
                Intent(Intent.ACTION_PICK).apply {
                    setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                },
                REQUEST_GALLERY
        )
    }

    private fun pickFromStorage() {
        targetFragment?.safeStartActivityForResult(
                newFilePickerIntent(
                    activity = activity,
                    initial = null,
                    allowMultiple = true,
                ),
                REQUEST_STORAGE
        )
    }

    companion object {
        const val REQUEST_CAMERA = 12120
        const val REQUEST_GALLERY = 12121
        const val REQUEST_STORAGE = 12122
        const val REQUEST_AUDIO = 12123

        fun newAddAttachmentDialog(target: FilesControlSet?): AddAttachmentDialog =
                AddAttachmentDialog().apply {
                    setTargetFragment(target, 0)
                }
    }
}