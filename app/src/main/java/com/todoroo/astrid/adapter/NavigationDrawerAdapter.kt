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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import org.tasks.LocalBroadcastManager
import org.tasks.activities.DragAndDropDiffer
import org.tasks.billing.Inventory
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.locale.Locale
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import java.util.*
import java.util.concurrent.Executors
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
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DragAndDropDiffer<FilterListItem, MutableList<FilterListItem>> {

    private lateinit var onClick: (FilterListItem?) -> Unit
    private var selected: Filter? = null
    override val channel = Channel<List<FilterListItem>>(Channel.UNLIMITED)
    override val updates: Queue<Pair<MutableList<FilterListItem>, DiffUtil.DiffResult?>> = LinkedList()
    override val scope: CoroutineScope =
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + Job())
    override var items = initializeDiffer(ArrayList())
    override var dragging = false

    fun setOnClick(onClick: (FilterListItem?) -> Unit) {
        this.onClick = onClick
    }

    fun save(outState: Bundle) {
        outState.putParcelable(TOKEN_SELECTED, selected)
    }

    fun restore(savedInstanceState: Bundle) {
        selected = savedInstanceState.getParcelable(TOKEN_SELECTED)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.dispose()
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = items.size

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
                    view,
                    activity as AppCompatActivity,
                    preferences,
                    googleTaskDao,
                    caldavDao,
                    localBroadcastManager)
            FilterListItem.Type.ACTION -> ActionViewHolder(activity, view) { onClickFilter(it) }
            else -> SeparatorViewHolder(view)
        }
    }

    private fun onClickFilter(filter: FilterListItem?) = onClick(if (filter == selected) null else filter)

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

    private fun getItem(position: Int) = items[position]

    override fun transform(list: List<FilterListItem>) = list.toMutableList()

    override fun diff(last: MutableList<FilterListItem>, next: MutableList<FilterListItem>) =
            DiffUtil.calculateDiff(DiffCallback(last, next))

    private class DiffCallback(val old: List<FilterListItem>, val new: List<FilterListItem>) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size

        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int) =
            old[oldPosition].areItemsTheSame(new[newPosition])

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int) =
            old[oldPosition].areContentsTheSame(new[newPosition])
    }

    companion object {
        private const val TOKEN_SELECTED = "token_selected"
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) =
            notifyItemRangeChanged(position, count, payload)

    override fun onMoved(fromPosition: Int, toPosition: Int) =
            notifyItemMoved(fromPosition, toPosition)

    override fun onInserted(position: Int, count: Int) =
            notifyItemRangeInserted(position, count)

    override fun onRemoved(position: Int, count: Int) =
            notifyItemRangeRemoved(position, count)
}