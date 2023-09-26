package org.tasks.fragments

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.activities.CameraActivity
import org.tasks.databinding.FragmentCommentBarBinding
import org.tasks.dialogs.DialogBuilder
import org.tasks.extensions.Context.hideKeyboard
import org.tasks.files.ImageHelper
import org.tasks.preferences.Device
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeColor
import org.tasks.ui.TaskEditViewModel
import javax.inject.Inject

@AndroidEntryPoint
class CommentBarFragment : Fragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var device: Device
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var themeColor: ThemeColor
    
    private lateinit var commentButton: View
    private lateinit var commentField: EditText
    private lateinit var pictureButton: ImageView
    private lateinit var commentBar: LinearLayout
    private var pendingCommentPicture: Uri? = null
    lateinit var viewModel: TaskEditViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireParentFragment())[TaskEditViewModel::class.java]
        val view = bind(container)
        createView(savedInstanceState)
        return view
    }

    private fun createView(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            val uri = savedInstanceState.getString(EXTRA_PICTURE)
            if (uri != null) {
                pendingCommentPicture = Uri.parse(uri)
                setPictureButtonToPendingPicture()
            }
            commentField.setText(savedInstanceState.getString(EXTRA_TEXT))
        }
        commentField.setHorizontallyScrolling(false)
        commentField.maxLines = Int.MAX_VALUE
        if (
            preferences.getBoolean(R.string.p_show_task_edit_comments, false) &&
            viewModel.isWritable
        ) {
            commentBar.visibility = View.VISIBLE
        }
        commentBar.setBackgroundColor(themeColor.primaryColor)
        resetPictureButton()
    }

    private fun bind(parent: ViewGroup?) =
        FragmentCommentBarBinding.inflate(layoutInflater, parent, false).let {
            commentButton = it.commentButton.apply {
                setOnClickListener { addClicked() }
            }
            commentField = it.commentField.apply {
                addTextChangedListener(
                    onTextChanged = { text, _, _, _ -> onTextChanged(text?.toString()) }
                )
                setOnEditorActionListener { _, _, event -> onEditorAction(event) }
            }
            pictureButton = it.picture.apply {
                setOnClickListener { onClickPicture() }
            }
            commentBar = it.updatesFooter
            it.root
        }

    private fun onTextChanged(s: String?) {
        commentButton.visibility = if (pendingCommentPicture == null && isNullOrEmpty(s.toString())) View.GONE else View.VISIBLE
    }

    private fun onEditorAction(key: KeyEvent?): Boolean {
        val actionId = key?.action ?: 0
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
            if (commentField.text.isNotEmpty() || pendingCommentPicture != null) {
                addComment()
                return true
            }
        }
        return false
    }

    private fun addClicked() {
        addComment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_TEXT, commentField.text.toString())
        if (pendingCommentPicture != null) {
            outState.putString(EXTRA_PICTURE, pendingCommentPicture.toString())
        }
    }

    private fun onClickPicture() {
        if (pendingCommentPicture == null) {
            showPictureLauncher(null)
        } else {
            showPictureLauncher {
                pendingCommentPicture = null
                resetPictureButton()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                pendingCommentPicture = data!!.data
                setPictureButtonToPendingPicture()
                commentField.requestFocus()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun addComment() {
        addComment(commentField.text.toString())
        activity.hideKeyboard(commentField)
    }

    private fun setPictureButtonToPendingPicture() {
        val bitmap = ImageHelper.sampleBitmap(
                activity,
                pendingCommentPicture,
                pictureButton.layoutParams.width,
                pictureButton.layoutParams.height)
        pictureButton.setImageBitmap(bitmap)
        commentButton.visibility = View.VISIBLE
    }

    private fun addComment(message: String) {
        val picture = pendingCommentPicture
        commentField.setText("")
        pendingCommentPicture = null
        resetPictureButton()
        viewModel.addComment(if (isNullOrEmpty(message)) " " else message, picture)
    }

    private fun resetPictureButton() {
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)
        val drawable = activity.getDrawable(R.drawable.ic_outline_photo_camera_24px)!!.mutate()
        drawable.setTint(typedValue.data)
        pictureButton.setImageDrawable(drawable)
    }

    private fun showPictureLauncher(clearImageOption: (() -> Unit)?) {
        val runnables: MutableList<() -> Unit> = ArrayList()
        val options: MutableList<String> = ArrayList()
        val cameraAvailable = device.hasCamera()
        if (cameraAvailable) {
            runnables.add {
                startActivityForResult(
                        Intent(activity, CameraActivity::class.java), REQUEST_CODE_CAMERA)
            }
            options.add(getString(R.string.take_a_picture))
        }
        if (clearImageOption != null) {
            runnables.add { clearImageOption() }
            options.add(getString(R.string.actfm_picture_clear))
        }
        if (runnables.size == 1) {
            runnables[0]()
        } else {
            val listener = DialogInterface.OnClickListener { d: DialogInterface, which: Int ->
                runnables[which]()
                d.dismiss()
            }

            // show a menu of available options
            dialogBuilder.newDialog().setItems(options, listener).show().setOwnerActivity(activity)
        }
    }

    companion object {
        private const val REQUEST_CODE_CAMERA = 60
        private const val EXTRA_TEXT = "extra_text"
        private const val EXTRA_PICTURE = "extra_picture"
    }
}