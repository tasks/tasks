package org.tasks.markdown

import android.text.Editable
import android.widget.EditText
import android.widget.TextView

class MarkdownDisabled : Markdown {
    override fun textWatcher(editText: EditText): ((Editable?) -> Unit)? = null

    override val enabled = false

    override fun setMarkdown(tv: TextView, markdown: String?) {
        tv.text = markdown
    }

    override fun toMarkdown(markdown: String?) = markdown
}