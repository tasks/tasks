package org.tasks.markdown

import android.text.Editable
import android.widget.EditText
import android.widget.TextView

interface Markdown {
    fun textWatcher(editText: EditText): ((Editable?) -> Unit)?

    val enabled: Boolean

    fun setMarkdown(tv: TextView, markdown: String?)

    fun toMarkdown(markdown: String?): CharSequence?
}