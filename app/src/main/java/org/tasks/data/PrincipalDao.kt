package org.tasks.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PrincipalDao {
    @Insert
    fun insert(principal: Principal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(principals: List<Principal>)

    @Query("""
DELETE
FROM principals
WHERE principal_list = :list
  AND principal NOT IN (:principals)""")
    fun deleteRemoved(list: Long, principals: List<String>)

    @Delete
    fun delete(principal: Principal)

    @Delete
    fun delete(principals: List<Principal>)

    @Query("SELECT * FROM principals")
    fun getAll(): List<Principal>

    @Query("SELECT * FROM principals WHERE principal_list = :id")
    fun getPrincipals(id: Long): LiveData<List<Principal>>
}