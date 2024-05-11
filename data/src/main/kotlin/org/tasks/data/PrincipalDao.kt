package org.tasks.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface PrincipalDao {
    @Insert
    fun insert(principal: Principal): Long

    @Insert
    fun insert(access: PrincipalAccess): Long

    @Update
    fun update(access: PrincipalAccess)

    @Query("""
DELETE
FROM principal_access
WHERE list = :list
  AND id NOT IN (:access)""")
    fun deleteRemoved(list: Long, access: List<Long>)

    @Delete
    fun delete(access: PrincipalAccess)

    @Transaction
    @Query("SELECT * FROM principal_access")
    fun getAll(): List<PrincipalWithAccess>

    fun getOrCreatePrincipal(account: CaldavAccount, href: String, displayName: String? = null) =
        findPrincipal(account.id, href)
            ?: Principal(account = account.id, href = href, displayName = displayName)
                .apply { id = insert(this) }

    fun getOrCreateAccess(
        calendar: CaldavCalendar,
        principal: Principal,
        invite: Int,
        access: Int,
    ): PrincipalAccess =
        findAccess(calendar.id, principal.id)
            ?.apply {
                if (this.access != access || this.invite != invite) {
                    this.access = access
                    this.invite = invite
                    update(this)
                }
            }
            ?: PrincipalAccess(
                principal = principal.id,
                list = calendar.id,
                invite = invite,
                access = access
            ).apply { id = insert(this) }

    @Query("SELECT * FROM principals WHERE account = :account AND href = :href")
    fun findPrincipal(account: Long, href: String): Principal?

    @Query("SELECT * FROM principal_access WHERE list = :list and principal = :principal")
    fun findAccess(list: Long, principal: Long): PrincipalAccess?

    @Transaction
    @Query("SELECT * FROM principal_access WHERE list = :id")
    fun getPrincipals(id: Long): LiveData<List<PrincipalWithAccess>>
}