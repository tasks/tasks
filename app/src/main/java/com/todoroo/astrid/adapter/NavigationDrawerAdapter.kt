/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import org.tasks.LocalBroadcastManager
import org.tasks.billing.Inventory
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.locale.Locale
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import javax.inject.Inject
import kotlin.math.max

class NavigationDrawerAdapter @Inject constructor(
        private val activity: Activity,
        private val locale: Locale,
        private val inventory: Inventory,
        private val colorProvider: ColorProvider,
        private val preferences: Preferences,
        private val googleTaskDao: GoogleTaskDao,
        private val caldavDao: CaldavDao,
        private val localBroadcastManager: LocalBroadcastManager)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var onClick: (FilterListItem?) -> Unit
    private var selected: Filter? = null
    private val differ = AsyncListDiffer(this, DiffCallback())

    fun setOnClick(onClick: (FilterListItem?) -> Unit) {
        this.onClick = onClick
    }

    fun save(outState: Bundle) {
        outState.putParcelable(TOKEN_SELECTED, selected)
    }

    fun restore(savedInstanceState: Bundle) {
        selected = savedInstanceState.getParcelable(TOKEN_SELECTED)
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = differ.currentList.size

    fun setSelected(selected: Filter?) {
        this.selected = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val type = FilterListItem.Type.values()[viewType]
        val view = LayoutInflater.from(parent.context).inflate(type.layout, parent, false)
        return when (type) {
            FilterListItem.Type.ITEM -> FilterViewHolder(
                        view, true, locale, activity, inventory, colorProvider) { onClickFilter(it) }
            FilterListItem.Type.SUBHEADER -> SubheaderViewHolder(
                        view, activity, preferences, googleTaskDao, caldavDao, localBroadcastManager)
            FilterListItem.Type.ACTION -> ActionViewHolder(activity, view) { onClickFilter(it) }
            else -> SeparatorViewHolder(view)
        }
    }

    private fun onClickFilter(filter: FilterListItem?) = onClick.invoke(if (filter == selected) null else filter)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.itemType) {
            FilterListItem.Type.ITEM ->
                (holder as FilterViewHolder).bind(item, item == selected, max(item.count, 0))
            FilterListItem.Type.ACTION -> (holder as ActionViewHolder).bind(item)
            FilterListItem.Type.SUBHEADER ->
                (holder as SubheaderViewHolder).bind((item as NavigationDrawerSubheader))
            else -> {}
        }
    }

    override fun getItemViewType(position: Int) = getItem(position).itemType.ordinal

    private fun getItem(position: Int) = differ.currentList[position]

    fun submitList(filterListItems: List<FilterListItem>) = differ.submitList(filterListItems)

    private class DiffCallback : DiffUtil.ItemCallback<FilterListItem>() {
        override fun areItemsTheSame(old: FilterListItem, new: FilterListItem) = old.areItemsTheSame(new)

        override fun areContentsTheSame(old: FilterListItem, new: FilterListItem) = old.areContentsTheSame(new)
    }

    companion object {
        private const val TOKEN_SELECTED = "token_selected"
    }
}