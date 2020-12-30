package org.tasks.preferences

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.tasks.R

class IconPreference(context: Context?, attrs: AttributeSet? = null) : Preference(context, attrs) {

    private var imageView: ImageView? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        imageView = holder?.findViewById(R.id.preference_icon) as ImageView?
        updateIcon()
    }

    var tint: Int? = null
        set(value) {
            field = value
            updateIcon()
        }

    var iconClickListener: View.OnClickListener? = null
        set(value) {
            field = value
            updateIcon()
        }

    var drawable: Drawable? = null
        set(value) {
            field = value
            updateIcon()
        }

    var iconVisible: Boolean = false
        set(value) {
            field = value
            updateIcon()
        }

    private fun updateIcon() {
        imageView?.visibility = if (iconVisible) View.VISIBLE else View.GONE
        drawable?.let { imageView?.setImageDrawable(drawable) }
        iconClickListener?.let { imageView?.setOnClickListener(it) }
        tint?.let { imageView?.setColorFilter(it) }
    }

    init {
        widgetLayoutResource = R.layout.preference_icon
    }
}