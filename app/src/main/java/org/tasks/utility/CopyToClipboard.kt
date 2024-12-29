package org.tasks.utility

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast.LENGTH_SHORT
import androidx.core.content.ContextCompat.getSystemService
import org.tasks.R
import org.tasks.extensions.Context.toast

fun copyToClipboard(context: Context, labelRes: Int, message: String) {
    val clipboard = getSystemService(context, ClipboardManager::class.java)
    if (clipboard != null) {
        val label = context.getString(labelRes)
        clipboard.setPrimaryClip(ClipData.newPlainText(label, message))
        context.toast(R.string.copied_to_clipboard, label, duration = LENGTH_SHORT)
    }
}
