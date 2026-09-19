package org.tasks.compose.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.filters.FilterProvider
import org.tasks.filters.FilterListItem
import org.tasks.filters.NavigationDrawerSubheader

class NavigationDrawerCustomizationViewModel(
    private val filterProvider: FilterProvider,
    private val filterDao: FilterDao,
    private val caldavDao: CaldavDao,
    private val tagDataDao: TagDataDao,
    private val locationDao: LocationDao,
) : ViewModel() {

    private val _items = MutableStateFlow<List<FilterListItem>>(emptyList())
    val items: StateFlow<List<FilterListItem>> = _items.asStateFlow()

    private val _collapsedSections = MutableStateFlow<Set<String>>(emptySet())
    val collapsedSections: StateFlow<Set<String>> = _collapsedSections.asStateFlow()

    init {
        loadItems(forceExpand = true)
    }

    fun toggleSectionCollapse(title: String?) {
        if (title == null) return
        val current = _collapsedSections.value.toMutableSet()
        val wasCollapsed = current.contains(title)
        if (wasCollapsed) {
            current.remove(title)
        } else {
            current.add(title)
        }
        _collapsedSections.value = current
        
        // Reload items when expanding a section to load the data
        if (wasCollapsed) {
            loadItems(forceExpand = true)
        }
    }

    fun loadItems(forceExpand: Boolean = false) {
        viewModelScope.launch {
            _items.value = filterProvider.drawerCustomizationItems(forceExpand = forceExpand)
        }
    }

    fun swapItems(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = _items.value.toMutableList()
            val fromItem = currentList.getOrNull(fromIndex)
            val toItem = currentList.getOrNull(toIndex)
            val shouldSwap = fromItem != null && toItem != null &&
                fromItem::class.java == toItem::class.java &&
                !(fromItem is org.tasks.filters.CaldavFilter &&
                    toItem is org.tasks.filters.CaldavFilter &&
                    fromItem.account != toItem.account)
            if (shouldSwap) {
                currentList.removeAt(fromIndex)
                currentList.add(toIndex, fromItem)
                _items.value = currentList
                updateOrders(currentList, fromItem)
            }
        }
    }

    fun resetOrders() {
        viewModelScope.launch {
            filterDao.resetOrders()
            caldavDao.resetOrders()
            tagDataDao.resetOrders()
            locationDao.resetOrders()
            loadItems()
        }
    }

    private suspend fun updateOrders(
        items: List<FilterListItem>,
        referenceItem: FilterListItem,
    ) {
        val itemsToUpdate = items.filter { it::class.java == referenceItem::class.java }

        // For CaldavFilter, filter by account
        val filteredItems = when (referenceItem) {
            is org.tasks.filters.CaldavFilter -> {
                itemsToUpdate.filterIsInstance<org.tasks.filters.CaldavFilter>()
                    .filter { it.account == referenceItem.account }
            }
            else -> itemsToUpdate
        }

        filteredItems.forEachIndexed { index, item ->
            when (item) {
                is org.tasks.filters.CaldavFilter -> {
                    item.calendar.id?.let { caldavDao.setOrder(it, index) }
                }
                is org.tasks.filters.TagFilter -> {
                    tagDataDao.setOrder(item.tagData.id!!, index)
                }
                is org.tasks.filters.CustomFilter -> {
                    filterDao.setOrder(item.id, index)
                }
                is org.tasks.filters.PlaceFilter -> {
                    locationDao.setOrder(item.place.id, index)
                }
            }
        }
    }
}
