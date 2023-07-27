package org.tasks.dialogs

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.ListAdapter
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AlertDialogBuilder internal constructor(private val context: Context) {
    private val builder: AlertDialog.Builder

    init {
        builder = MaterialAlertDialogBuilder(context)
    }

    fun setMessage(message: Int, vararg formatArgs: Any?): AlertDialogBuilder {
        return setMessage(context.getString(message, *formatArgs))
    }

    fun setMessage(message: String?): AlertDialogBuilder {
        builder.setMessage(message)
        return this
    }

    fun setPositiveButton(
        ok: Int, onClickListener: DialogInterface.OnClickListener?
    ): AlertDialogBuilder {
        builder.setPositiveButton(ok, onClickListener)
        return this
    }

    fun setNegativeButton(
        cancel: Int, onClickListener: DialogInterface.OnClickListener?
    ): AlertDialogBuilder {
        builder.setNegativeButton(cancel, onClickListener)
        return this
    }

    fun setTitle(title: Int): AlertDialogBuilder {
        builder.setTitle(title)
        return this
    }

    fun setTitle(title: Int, vararg formatArgs: Any?): AlertDialogBuilder {
        builder.setTitle(context.getString(title, *formatArgs))
        return this
    }

    fun setItems(
        strings: List<String>, onClickListener: DialogInterface.OnClickListener?
    ): AlertDialogBuilder {
        return setItems(strings.toTypedArray(), onClickListener)
    }

    fun setItems(
        strings: Array<String>, onClickListener: DialogInterface.OnClickListener?
    ): AlertDialogBuilder {
        builder.setItems(strings.clone(), onClickListener)
        return this
    }

    fun setView(dialogView: View?): AlertDialogBuilder {
        builder.setView(dialogView)
        return this
    }

    fun setContent(content: @Composable () -> Unit): AlertDialogBuilder {
        builder.setView(ComposeView(context)
            .apply {
                setContent {
                    MdcTheme {
                        content()
                    }
                }
            }
        )
        return this
    }

    fun setOnCancelListener(onCancelListener: DialogInterface.OnCancelListener?): AlertDialogBuilder {
        builder.setOnCancelListener(onCancelListener)
        return this
    }

    fun setSingleChoiceItems(
        strings: List<String>, selectedIndex: Int, onClickListener: DialogInterface.OnClickListener?
    ): AlertDialogBuilder {
        return setSingleChoiceItems(strings.toTypedArray(), selectedIndex, onClickListener)
    }

    fun setSingleChoiceItems(
        strings: Array<String>?,
        selectedIndex: Int,
        onClickListener: DialogInterface.OnClickListener?
    ): AlertDialogBuilder {
        builder.setSingleChoiceItems(strings, selectedIndex, onClickListener)
        return this
    }

    fun setSingleChoiceItems(
        adapter: ListAdapter?, selectedIndex: Int, onClickListener: DialogInterface.OnClickListener?
    ): AlertDialogBuilder {
        builder.setSingleChoiceItems(adapter, selectedIndex, onClickListener)
        return this
    }

    fun setNeutralButton(
        resId: Int, onClickListener: DialogInterface.OnClickListener?
    ): AlertDialogBuilder {
        builder.setNeutralButton(resId, onClickListener)
        return this
    }

    fun setTitle(title: String?): AlertDialogBuilder {
        builder.setTitle(title)
        return this
    }

    fun setCancelable(cancelable: Boolean): AlertDialogBuilder {
        builder.setCancelable(cancelable)
        return this
    }

    fun create(): AlertDialog {
        return builder.create()
    }

    fun show(): AlertDialog {
        val dialog = create()
        dialog.show()
        return dialog
    }
}