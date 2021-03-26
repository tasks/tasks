package org.tasks.ui

import android.text.Editable
import android.text.TextWatcher

abstract class OnTextChanged : TextWatcher {
    final override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    final override fun afterTextChanged(s: Editable?) {}
    final override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
        onTextChanged(s)

    abstract fun onTextChanged(text: CharSequence?)
}