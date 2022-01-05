/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import com.todoroo.astrid.api.FilterListItem.Type.*
import org.tasks.billing.Inventory
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.locale.Locale
import org.tasks.themes.ColorProvider
import java.util.*
import javax.inject.Inject

class FilterAdapter @Inject constructor(
        private val activity: Activity,
        private val locale: Locale,
        private val inventory: Inventory,
        private val colorProvider: ColorProvider,
        private val subheaderClickHandler: SubheaderClickHandler,
) : BaseAdapter() {
    private var selected: Filter? = null
    private var items: List<FilterListItem> = ArrayList()

    fun save(outState: Bundle) {
        outState.putParcelableArrayList(TOKEN_FILTERS, getItems())
        outState.putParcelable(TOKEN_SELECTED, selected)
    }

    fun restore(savedInstanceState: Bundle) {
        items = savedInstanceState.getParcelableArrayList(TOKEN_FILTERS) ?: ArrayList()
        selected = savedInstanceState.getParcelable(TOKEN_SELECTED)
    }

    fun setData(items: List<FilterListItem>, selected: Filter?) {
        AndroidUtilities.assertMainThread()
        this.items = items
        this.selected = selected
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        AndroidUtilities.assertMainThread()
        return items.size
    }

    override fun getItem(position: Int): FilterListItem {
        AndroidUtilities.assertMainThread()
        return items[position]
    }

    override fun getItemId(position: Int) = position.toLong()

    /** Create or reuse a view  */
    private fun newView(convertView: View?, parent: ViewGroup, viewType: FilterListItem.Type): View {
        return if (convertView != null) {
            convertView
        } else {
            val newView = LayoutInflater.from(parent.context).inflate(viewType.layout, parent, false)
            newView.tag = when (viewType) {
                ITEM -> FilterViewHolder(
                        newView, false, locale, activity, inventory, colorProvider, null)
                ACTION -> ActionViewHolder(activity, newView, null)
                SEPARATOR -> SeparatorViewHolder(newView)
                SUBHEADER -> SubheaderViewHolder(newView, subheaderClickHandler)
            }
            newView
        }
    }

    private fun getItems(): ArrayList<FilterListItem> {
        AndroidUtilities.assertMainThread()
        return ArrayList(items)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val view = newView(convertView, parent, item.itemType)
        val viewHolder = view.tag as RecyclerView.ViewHolder
        when (item.itemType) {
            ITEM -> (viewHolder as FilterViewHolder).bind(item, item == selected, 0)
            ACTION -> (viewHolder as ActionViewHolder).bind(item)
            SUBHEADER ->
                (viewHolder as SubheaderViewHolder).bind((item as NavigationDrawerSubheader))
            else -> {}
        }
        return view
    }

    override fun getViewTypeCount() = VIEW_TYPE_COUNT

    override fun isEnabled(position: Int) = getItem(position).itemType == ITEM

    override fun getItemViewType(position: Int) = getItem(position).itemType.ordinal

    companion object {
        private const val TOKEN_FILTERS = "token_filters"
        private const val TOKEN_SELECTED = "token_selected"
        private val VIEW_TYPE_COUNT = values().size
    }
}