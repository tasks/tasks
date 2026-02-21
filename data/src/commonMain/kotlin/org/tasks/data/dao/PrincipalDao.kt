package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.tasks.data.PrincipalWithAccess
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_INVALID
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_NO_RESPONSE
import org.tasks.data.entity.Principal
import org.tasks.data.entity.PrincipalAccess

@Dao
abstract class PrincipalDao {
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

    @Query("DELETE FROM principal_access WHERE id = :id")
    abstract suspend fun deleteAccessById(id: Long)

    @Query("""
        SELECT pa.id, pa.list, pa.invite, pa.access, p.href, p.display_name
        FROM principal_access pa
        INNER JOIN principals p ON pa.principal = p.id
    """)
    abstract suspend fun getAll(): List<PrincipalWithAccess>

    suspend fun getOrCreatePrincipal(account: CaldavAccount, href: String, displayName: String? = null) =
        findPrincipal(account.id, href)
            ?.apply {
                if (displayName != null && this.displayName != displayName) {
                    this.displayName = displayName
                    updatePrincipal(this)
                }
            }
            ?: Principal(account = account.id, href = href, displayName = displayName)
                .apply { id = insert(this) }

    @Update
    abstract suspend fun updatePrincipal(principal: Principal)

    suspend fun getOrCreateAccess(
        calendar: CaldavCalendar,
        principal: Principal,
        invite: Int,
        access: Int,
    ): PrincipalAccess =
        findAccess(calendar.id, principal.id)
            ?.apply {
                // Don't let a stale sync overwrite a known-good status:
                // INVITE_NO_RESPONSE means we sent an invite, while
                // INVITE_INVALID just means the mailto: principal wasn't
                // resolved yet on the server at PROPFIND time.
                val newInvite =
                    if (this.invite == INVITE_NO_RESPONSE && invite == INVITE_INVALID)
                        this.invite
                    else
                        invite
                if (this.access != access || this.invite != newInvite) {
                    this.access = access
                    this.invite = newInvite
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

    @Query("""
        SELECT pa.id, pa.list, pa.invite, pa.access, p.href, p.display_name
        FROM principal_access pa
        INNER JOIN principals p ON pa.principal = p.id
        WHERE pa.list = :id
    """)
    abstract fun getPrincipals(id: Long): Flow<List<PrincipalWithAccess>>

    @Query("""
        SELECT p.display_name
        FROM principal_access pa
        INNER JOIN principals p ON pa.principal = p.id
        WHERE pa.list = :listId AND pa.access = 0
        LIMIT 1
    """)
    abstract suspend fun getOwnerName(listId: Long): String?

}
