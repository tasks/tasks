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

    @JvmOverloads
    fun markdown(linkify: Boolean = false, force: Boolean = false) =
        if (force || preferences.getBoolean(R.string.p_markdown, false)) {
            Markwon(context, linkify)
        } else {
            MarkdownDisabled()
        }
}