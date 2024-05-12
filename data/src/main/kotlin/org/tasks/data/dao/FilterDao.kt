package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.tasks.data.entity.Filter
import org.tasks.data.NO_ORDER

@Dao
interface FilterDao {
    @Update
    suspend fun update(filter: Filter)

    @Query("DELETE FROM filters WHERE _id = :id")
    suspend fun delete(id: Long)

    @Delete
    suspend fun delete(filter: Filter)

    @Query("SELECT * FROM filters WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun getByName(title: String): Filter?

    @Insert
    suspend fun insert(filter: Filter): Long

    @Query("SELECT * FROM filters")
    suspend fun getFilters(): List<Filter>

    @Query("SELECT * FROM filters WHERE _id = :id LIMIT 1")
    suspend fun getById(id: Long): Filter?

    @Query("UPDATE filters SET f_order = $NO_ORDER")
    suspend fun resetOrders()

    @Query("UPDATE filters SET f_order = :order WHERE _id = :id")
    suspend fun setOrder(id: Long, order: Int)
}