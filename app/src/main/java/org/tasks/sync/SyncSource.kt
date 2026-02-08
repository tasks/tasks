package org.tasks.sync

enum class SyncSource(val showIndicator: Boolean) {
    NONE(false),
    USER_INITIATED(true),
    PUSH_NOTIFICATION(false),
    CONTENT_OBSERVER(true),
    BACKGROUND(false),
    TASK_CHANGE(true),
    APP_BACKGROUND(false),
    APP_RESUME(false),
    ACCOUNT_ADDED(true),
    ;

    fun upgrade(other: SyncSource): SyncSource =
        if (other.showIndicator && !this.showIndicator) other else this

    companion object {
        fun fromString(value: String?): SyncSource =
            try {
                valueOf(value ?: NONE.name)
            } catch (_: IllegalArgumentException) {
                NONE
            }
    }
}
