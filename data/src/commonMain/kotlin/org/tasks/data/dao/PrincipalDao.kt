package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.tasks.data.PrincipalWithAccess
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Principal
import org.tasks.data.entity.PrincipalAccess
import org.tasks.data.withTransaction

@Dao
abstract class PrincipalDao(private val database: Database) {
    @Insert
    abstract suspend fun insert(principal: Principal): Long

    @Insert
    abstract suspend fun insert(access: PrincipalAccess): Long

    @Update
    abstract suspend fun update(access: PrincipalAccess)

    @Query("""
DELETE
FROM principal_access
WHERE list = :list
  AND id NOT IN (:access)""")
    abstract suspend fun deleteRemoved(list: Long, access: List<Long>)

    @Delete
    abstract suspend fun delete(access: PrincipalAccess)

    suspend fun getAll(): List<PrincipalWithAccess> = database.withTransaction {
        getAllInternal()
    }

    @Query("SELECT * FROM principal_access")
    internal abstract suspend fun getAllInternal(): List<PrincipalWithAccess>

    suspend fun getOrCreatePrincipal(account: CaldavAccount, href: String, displayName: String? = null) =
        findPrincipal(account.id, href)
            ?: Principal(account = account.id, href = href, displayName = displayName)
                .apply { id = insert(this) }

    suspend fun getOrCreateAccess(
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
    abstract suspend fun findPrincipal(account: Long, href: String): Principal?

    @Query("SELECT * FROM principal_access WHERE list = :list and principal = :principal")
    abstract suspend fun findAccess(list: Long, principal: Long): PrincipalAccess?

    @Query("SELECT * FROM principal_access WHERE list = :id")
    abstract fun getPrincipals(id: Long): Flow<List<PrincipalWithAccess>>

}