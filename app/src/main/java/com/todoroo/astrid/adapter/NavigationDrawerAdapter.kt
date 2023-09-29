/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import org.tasks.activities.DragAndDropDiffer
import org.tasks.billing.Inventory
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.themes.ColorProvider
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.math.max

class NavigationDrawerAdapter @Inject constructor(
    private val activity: Activity,
    private val locale: Locale,
    private val inventory: Inventory,
    private val colorProvider: ColorProvider,
    private val subheaderClickHandler: SubheaderClickHandler,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    DragAndDropDiffer<FilterListItem, MutableList<FilterListItem>> {

    private lateinit var onClick: (FilterListItem?) -> Unit
    override val channel = Channel<List<FilterListItem>>(Channel.UNLIMITED)
    override val updates: Queue<Pair<MutableList<FilterListItem>, DiffUtil.DiffResult?>> = LinkedList()
    override val scope: CoroutineScope =
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + Job())
    override var items = initializeDiffer(ArrayList())
    override var dragging = false

    fun setOnClick(onClick: (FilterListItem?) -> Unit) {
        this.onClick = onClick
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.dispose()
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val type = FilterListItem.Type.values()[viewType]
        val view = LayoutInflater.from(parent.context).inflate(type.layout, parent, false)
        return when (type) {
            FilterListItem.Type.ITEM -> FilterViewHolder(
                        view, true, locale, activity, inventory, colorProvider) { onClickFilter(it) }
            FilterListItem.Type.SUBHEADER -> SubheaderViewHolder(view, subheaderClickHandler)
        }
    }

    private fun onClickFilter(filter: FilterListItem?) =
        onClick(filter)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.itemType) {
            FilterListItem.Type.ITEM ->
                (holder as FilterViewHolder).bind(item as Filter, false, max(item.count, 0))
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
            old[oldPosition] == new[newPosition]
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