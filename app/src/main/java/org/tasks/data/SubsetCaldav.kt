package org.tasks.data

class SubsetCaldav {
    var cd_id: Long = 0
    var cd_calendar: String? = null
    var cd_remote_parent: String? = null
    var gt_parent: Long = 0
    var cd_order: Long? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SubsetCaldav) return false

        if (cd_id != other.cd_id) return false
        if (cd_calendar != other.cd_calendar) return false
        if (cd_remote_parent != other.cd_remote_parent) return false
        if (gt_parent != other.gt_parent) return false
        if (cd_order != other.cd_order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cd_id.hashCode()
        result = 31 * result + (cd_calendar?.hashCode() ?: 0)
        result = 31 * result + (cd_remote_parent?.hashCode() ?: 0)
        result = 31 * result + (gt_parent.hashCode())
        result = 31 * result + (cd_order.hashCode())
        return result
    }

    override fun toString(): String =
            "SubsetCaldav(cd_id=$cd_id, cd_calendar=$cd_calendar, cd_remote_parent=$cd_remote_parent, gt_parent=$gt_parent, cd_order=$cd_order)"
}