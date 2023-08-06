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
import androidx.core.net.toUri
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

    fun setMovementMethod(
        tv: TextView,
        linkClickHandler: ((url: String) -> Boolean) = { false },
        rowClickHandler: (() -> Unit) = {}
    ) {
        tv.setOnClickListener {
            if (!tv.hasSelection()) {
                rowClickHandler()
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
                    ClickHandlingURLSpan(span.url, linkClickHandler, rowClickHandler),
                    start,
                    end,
                    0
                )

            }
        }
    }

    private inner class ClickHandlingURLSpan(
        url: String?,
        private val linkClickHandler: ((String) -> Boolean),
        private val rowClickHandler: (() -> Unit),
    ) : URLSpan(url) {
        override fun onClick(widget: View) {
            if (linkClickHandler(url)) {
                return
            }
            val uri = url.toUri().takeUnless { it.scheme.isNullOrBlank() } ?: "https://$url".toUri()
            val title = when (uri.scheme) {
                "tel", "mailto" -> uri.encodedSchemeSpecificPart
                "geo" -> {
                    uri
                        .encodedQuery
                        ?.replaceFirst("q=".toRegex(), "")
                        ?.let {
                            try {
                                URLDecoder.decode(it, "utf-8")
                            } catch (ignored: UnsupportedEncodingException) {
                                it
                            }
                        }
                }
                else -> url
            }
            dialogBuilder
                .newDialog(title)
                .setItems(
                    listOf(uri.action, R.string.TAd_actionEditTask).map { context.getString(it) }
                ) { _, selected ->
                    if (selected == 0) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } else {
                        rowClickHandler()
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

        val Uri.action: Int
            get() = if (scheme == "tel") R.string.action_call else R.string.action_open
    }
}