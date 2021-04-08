package org.tasks.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.util.Linkify
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.text.util.LinkifyCompat
import dagger.hilt.android.qualifiers.ActivityContext
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.tasks.R
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import javax.inject.Inject

class Linkify @Inject constructor(
    @ActivityContext private val context: Context,
    private val dialogBuilder: DialogBuilder
) {
    fun linkify(tv: TextView) {
        if (tv.length() == 0) {
            return
        }
        safeLinkify(tv)
        setMovementMethod(tv)
    }

    fun setMovementMethod(tv: TextView, handle: (() -> Unit)? = null) {
        val blmm = BetterLinkMovementMethod.newInstance().apply {
            setOnLinkClickListener { _, url -> onClick(url, handle ?: {}) }
            setOnLinkLongClickListener { _, _ -> tv.performLongClick() }
        }
        tv.movementMethod = blmm
        tv.setOnTouchListener { _, event ->
            val text = tv.text
            when {
                text is Spannable && blmm.onTouchEvent(tv, text, event) -> true
                event.action == MotionEvent.ACTION_UP -> {
                    handle?.invoke()
                    false
                }
                else -> false
            }
        }
    }

    fun onClick(url: String, onEdit: () -> Unit): Boolean {
        var title: String?
        val edit = context.getString(R.string.TAd_actionEditTask)
        val action: String
        val uri = Uri.parse(url).let {
            if (it.scheme.isNullOrBlank()) {
                Uri.parse("https://$url")
            } else {
                it
            }
        }
        when (uri.scheme) {
            "tel" -> {
                title = uri.encodedSchemeSpecificPart
                action = context.getString(R.string.action_call)
            }
            "mailto" -> {
                title = uri.encodedSchemeSpecificPart
                action = context.getString(R.string.action_open)
            }
            "geo" -> {
                title = uri.encodedQuery!!.replaceFirst("q=".toRegex(), "")
                try {
                    title = URLDecoder.decode(title, "utf-8")
                } catch (ignored: UnsupportedEncodingException) {
                }
                action = context.getString(R.string.action_open)
            }
            else -> {
                title = url
                action = context.getString(R.string.action_open)
            }
        }
        dialogBuilder
            .newDialog(title)
            .setItems(listOf(action, edit)) { _, selected ->
                if (selected == 0) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } else {
                    onEdit()
                }
            }
            .show()
        return true
    }

    companion object {
        fun safeLinkify(textView: TextView?, mask: Int = Linkify.ALL) {
            try {
                LinkifyCompat.addLinks(textView!!, mask)
            } catch (e: UnsatisfiedLinkError) {
                Timber.e(e)
            }
        }
    }
}