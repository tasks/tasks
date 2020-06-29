package org.tasks.data

import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class FilterDaoBlocking @Inject constructor(private val dao: FilterDao) {
    fun update(filter: Filter) = runBlocking {
        dao.update(filter)
    }

    fun delete(id: Long) = runBlocking {
        dao.delete(id)
    }

    fun getByName(title: String): Filter? = runBlocking {
        dao.getByName(title)
    }

    fun insert(filter: Filter): Long = runBlocking {
        dao.insert(filter)
    }

    fun getFilters(): List<Filter> = runBlocking {
        dao.getFilters()
    }

    fun getById(id: Long): Filter? = runBlocking {
        dao.getById(id)
    }

    fun getAll(): List<Filter> = runBlocking {
        dao.getAll()
    }

    fun resetOrders() = runBlocking {
        dao.resetOrders()
    }

    fun setOrder(id: Long, order: Int) = runBlocking {
        dao.setOrder(id, order)
    }
}