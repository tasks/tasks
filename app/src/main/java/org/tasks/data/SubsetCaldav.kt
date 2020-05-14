package org.tasks.data

class SubsetCaldav {
    var cd_id: Long = 0
    var cd_calendar: String? = null
    var cd_remote_parent: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SubsetCaldav) return false

        if (cd_id != other.cd_id) return false
        if (cd_calendar != other.cd_calendar) return false
        if (cd_remote_parent != other.cd_remote_parent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cd_id.hashCode()
        result = 31 * result + (cd_calendar?.hashCode() ?: 0)
        result = 31 * result + (cd_remote_parent?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "SubsetCaldav(id=$cd_id, calendar=$cd_calendar, remoteParent=$cd_remote_parent)"
    }
}