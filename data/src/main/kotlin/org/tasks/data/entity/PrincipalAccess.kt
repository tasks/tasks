package org.tasks.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_UNKNOWN
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_UNKNOWN

@Entity(
    tableName = "principal_access",
    foreignKeys = [
        ForeignKey(
            entity = Principal::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("principal"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CaldavCalendar::class,
            parentColumns = arrayOf("cdl_id"),
            childColumns = arrayOf("list"),
            onDelete = ForeignKey.CASCADE
        )],
    indices = [
        Index(value = ["list", "principal"], unique = true),
        Index(value = ["principal"])
    ]
)
data class PrincipalAccess(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val principal: Long = 0,
    val list: Long,
    var invite: Int = INVITE_UNKNOWN,
    var access: Int = ACCESS_UNKNOWN
)