package org.tasks.data

import androidx.room.*

@Entity(
    tableName = "principals",
    foreignKeys = [ForeignKey(
        entity = CaldavCalendar::class,
        parentColumns = arrayOf("cdl_id"),
        childColumns = arrayOf("principal_list"),
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["principal_list", "principal"], unique = true)]
)
class Principal {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "principal_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "principal_list")
    var list: Long = 0

    @ColumnInfo(name = "principal")
    var principal: String? = null

    @ColumnInfo(name = "display_name")
    var displayName: String? = null

    @ColumnInfo(name = "invite")
    var inviteStatus: Int = CaldavCalendar.INVITE_UNKNOWN

    @ColumnInfo(name = "access")
    var access: Int = CaldavCalendar.ACCESS_UNKNOWN

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Principal

        if (id != other.id) return false
        if (list != other.list) return false
        if (principal != other.principal) return false
        if (displayName != other.displayName) return false
        if (inviteStatus != other.inviteStatus) return false
        if (access != other.access) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + list.hashCode()
        result = 31 * result + (principal?.hashCode() ?: 0)
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + inviteStatus
        result = 31 * result + access
        return result
    }

    override fun toString(): String {
        return "Principal(id=$id, list=$list, principal=$principal, displayName=$displayName, inviteStatus=$inviteStatus, access=$access)"
    }
}