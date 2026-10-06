package org.tasks.markdown

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.preferences.Preferences
import javax.inject.Inject

class MarkdownProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences
){
    fun markdown(linkify: Int) = markdown(preferences.getBoolean(linkify, false))

    fun markdown(linkify: Int, checkboxColors: CheckboxColors?) =
        markdown(preferences.getBoolean(linkify, false), force = false, checkboxColors)

    @JvmOverloads
    fun markdown(linkify: Boolean = false, force: Boolean = false) =
        markdown(linkify, force, checkboxColors = null)

    // A separate overload rather than a third defaulted parameter, so existing callers and the
    // mocks that stub markdown(Boolean, Boolean) keep resolving to the same method.
    fun markdown(linkify: Boolean, force: Boolean, checkboxColors: CheckboxColors?): Markdown =
        if (force || preferences.getBoolean(R.string.p_markdown, false)) {
            Markwon(context, linkify, checkboxColors)
        } else {
            MarkdownDisabled()
        }
}