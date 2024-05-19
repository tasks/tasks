package org.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "principals",
    foreignKeys = [
        ForeignKey(
            entity = CaldavAccount::class,
            parentColumns = arrayOf("cda_id"),
            childColumns = arrayOf("account"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["account", "href"], unique = true)]
)
data class Principal(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val account: Long,
    val href: String,
    var email: String? = null,
    @ColumnInfo(name = "display_name") var displayName: String? = null
) {
    val name: String
        get() = displayName
            ?: href
                .replace(MAILTO, "")
                .replaceFirst(LAST_SEGMENT, "$1")

    companion object {
        private val MAILTO = "^mailto:".toRegex()
        private val LAST_SEGMENT = ".*/([^/]+).*".toRegex()
    }
}