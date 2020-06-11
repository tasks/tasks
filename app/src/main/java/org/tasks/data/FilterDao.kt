package org.tasks.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.todoroo.astrid.api.FilterListItem.NO_ORDER

@Dao
interface FilterDao {
    @Update
    fun update(filter: Filter)

    @Query("DELETE FROM filters WHERE _id = :id")
    fun delete(id: Long)

    @Query("SELECT * FROM filters WHERE title = :title COLLATE NOCASE LIMIT 1")
    fun getByName(title: String): Filter?

    @Insert
    fun insert(filter: Filter): Long

    @Query("SELECT * FROM filters")
    fun getFilters(): List<Filter>

    @Query("SELECT * FROM filters WHERE _id = :id LIMIT 1")
    fun getById(id: Long): Filter?

    @Query("SELECT * FROM filters")
    fun getAll(): List<Filter>

    @Query("UPDATE filters SET f_order = $NO_ORDER")
    fun resetOrders()

    @Query("UPDATE filters SET f_order = :order WHERE _id = :id")
    fun setOrder(id: Long, order: Int)
}