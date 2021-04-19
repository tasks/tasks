package org.tasks.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.core.text.util.LinkifyCompat
import dagger.hilt.android.qualifiers.ActivityContext
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
        tv.setOnClickListener {
            if (!tv.hasSelection()) {
                handle?.invoke()
            }
        }
        val text = tv.text
        if (text is SpannableStringBuilder || text is SpannableString) {
            val spannable = text as Spannable
            val spans = spannable.getSpans(0, text.length, URLSpan::class.java)
            for (span in spans) {
                val start = spannable.getSpanStart(span)
                val end = spannable.getSpanEnd(span)
                spannable.removeSpan(span)
                spannable.setSpan(
                    ClickHandlingURLSpan(span.url, handle),
                    start,
                    end,
                    0
                )

            }
        }
    }

    private inner class ClickHandlingURLSpan constructor(
        url: String?,
        private val onEdit: (() -> Unit)? = null
    ) : URLSpan(url) {
        override fun onClick(widget: View) {
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
                        onEdit?.invoke()
                    }
                }
                .show()
        }
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