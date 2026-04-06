package org.tasks.sync

enum class SyncSource(val showIndicator: Boolean, val immediate: Boolean = true) {
    NONE(false),
    USER_INITIATED(true),
    PUSH_NOTIFICATION(false),
    CONTENT_OBSERVER(true),
    BACKGROUND(false),
    TASK_CHANGE(showIndicator = true, immediate = false),
    APP_BACKGROUND(false),
    APP_RESUME(false),
    ACCOUNT_ADDED(true),
    PURCHASE_COMPLETED(true),
    SHARING_CHANGE(true),
    ;

    fun upgrade(other: SyncSource): SyncSource = when {
        other.showIndicator && !this.showIndicator -> other
        other.immediate && !this.immediate -> other
        else -> this
    }

    companion object {
        fun fromString(value: String?): SyncSource =
            try {
                valueOf(value ?: NONE.name)
            } catch (_: IllegalArgumentException) {
                NONE
            }
    }
}
