package org.tasks.data

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.todoroo.andlib.data.Table

@Entity(tableName = "google_task_accounts")
class GoogleTaskAccount : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "gta_id")
    @Transient
    var id: Long = 0

    @ColumnInfo(name = "gta_account")
    var account: String? = null

    @ColumnInfo(name = "gta_error")
    @Transient
    var error: String? = ""

    @ColumnInfo(name = "gta_etag")
    var etag: String? = null

    @ColumnInfo(name = "gta_collapsed")
    var isCollapsed = false

    constructor()

    @Ignore
    constructor(source: Parcel) {
        id = source.readLong()
        account = source.readString()
        error = source.readString()
        etag = source.readString()
        isCollapsed = ParcelCompat.readBoolean(source)
    }

    @Ignore
    constructor(account: String?) {
        this.account = account
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        with(dest) {
            writeLong(id)
            writeString(account)
            writeString(error)
            writeString(etag)
            ParcelCompat.writeBoolean(this, isCollapsed)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoogleTaskAccount) return false

        if (id != other.id) return false
        if (account != other.account) return false
        if (error != other.error) return false
        if (etag != other.etag) return false
        if (isCollapsed != other.isCollapsed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (account?.hashCode() ?: 0)
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + (etag?.hashCode() ?: 0)
        result = 31 * result + isCollapsed.hashCode()
        return result
    }

    override fun toString(): String =
            "GoogleTaskAccount(id=$id, account=$account, error=$error, etag=$etag, isCollapsed=$isCollapsed)"

    val hasError: Boolean
        get() = !error.isNullOrBlank()

    companion object {
        val TABLE = Table("google_task_accounts")
        val ACCOUNT = TABLE.column("gta_account")

        @JvmField val CREATOR: Parcelable.Creator<GoogleTaskAccount> = object : Parcelable.Creator<GoogleTaskAccount> {
            override fun createFromParcel(source: Parcel): GoogleTaskAccount = GoogleTaskAccount(source)

            override fun newArray(size: Int): Array<GoogleTaskAccount?> = arrayOfNulls(size)
        }
    }
}