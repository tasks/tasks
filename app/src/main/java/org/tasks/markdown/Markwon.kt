package org.tasks.markdown

import android.content.Context
import android.text.util.Linkify.*
import android.widget.EditText
import android.widget.TextView
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import java.util.concurrent.Executors

class Markwon(context: Context, linkify: Boolean) : Markdown {
    private val markwon: io.noties.markwon.Markwon

    override fun textWatcher(editText: EditText) =
        MarkwonEditorTextWatcher.withPreRender(
            MarkwonEditor.create(markwon), Executors.newCachedThreadPool(), editText
        )::afterTextChanged

    override val enabled = true

    override fun setMarkdown(tv: TextView, markdown: String?) {
        if (markdown?.isNotBlank() == true) {
            markwon.setMarkdown(tv, markdown)
        } else {
            tv.text = markdown
        }
    }

    override fun toMarkdown(markdown: String?) = markdown?.let { markwon.toMarkdown(it) }

    init {
        val builder = io.noties.markwon.Markwon
            .builder(context)
            .usePlugins(
                listOf(
                    TaskListPlugin.create(context),
                    TablePlugin.create(context),
                    StrikethroughPlugin.create()
                )
            )
        if (linkify) {
            builder.usePlugin(
                LinkifyPlugin.create(WEB_URLS or EMAIL_ADDRESSES or PHONE_NUMBERS, true)
            )
        }
        markwon = builder.build()
    }
}