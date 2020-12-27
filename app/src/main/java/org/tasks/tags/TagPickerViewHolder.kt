package org.tasks.tags

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import butterknife.OnClick
import org.tasks.R
import org.tasks.data.TagData
import org.tasks.tags.CheckBoxTriStates
import org.tasks.themes.DrawableUtil

class TagPickerViewHolder internal constructor(
        private val context: Context,
        view: View,
        private val callback: (TagData, TagPickerViewHolder) -> Unit
) : RecyclerView.ViewHolder(view) {

    val isChecked: Boolean
        get() = checkBox.isChecked

    @BindView(R.id.text)
    lateinit var text: TextView

    @BindView(R.id.checkbox)
    lateinit var checkBox: CheckBoxTriStates

    private var tagData: TagData? = null

    @OnClick(R.id.tag_row)
    fun onClickRow() {
        if (tagData!!.id == null) {
            callback(tagData!!, this)
        } else {
            checkBox.toggle()
        }
    }

    @OnCheckedChanged(R.id.checkbox)
    fun onCheckedChanged() {
        callback(tagData!!, this)
    }

    fun bind(
            tagData: TagData, color: Int, icon: Int?, state: CheckBoxTriStates.State) {
        var icon = icon
        this.tagData = tagData
        if (tagData.id == null) {
            text.text = context.getString(R.string.create_new_tag, tagData.name)
            icon = R.drawable.ic_outline_add_24px
            checkBox.visibility = View.GONE
        } else {
            text.text = tagData.name
            if (state == CheckBoxTriStates.State.CHECKED) {
                checkBox.isChecked = true
            } else {
                updateCheckbox(state)
            }
            if (icon == null) {
                icon = R.drawable.ic_outline_label_24px
            }
        }
        DrawableUtil.setLeftDrawable(context, text, icon)
        DrawableUtil.setTint(DrawableUtil.getLeftDrawable(text), color)
    }

    fun updateCheckbox(state: CheckBoxTriStates.State) {
        checkBox.setState(state, false)
        checkBox.visibility = View.VISIBLE
    }

    init {
        ButterKnife.bind(this, view)
    }
}