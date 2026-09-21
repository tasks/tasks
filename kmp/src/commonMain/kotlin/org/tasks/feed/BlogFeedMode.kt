package org.tasks.feed

enum class BlogFeedMode(val value: Int) {
    NONE(0),
    ANNOUNCEMENTS(1),
    ALL(2);

    companion object {
        fun fromValue(value: Int): BlogFeedMode =
            entries.firstOrNull { it.value == value } ?: ANNOUNCEMENTS
    }
}
