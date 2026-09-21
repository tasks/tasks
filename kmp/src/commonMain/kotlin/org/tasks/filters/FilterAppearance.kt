package org.tasks.filters

/**
 * The parts of a [Filter] that a chip or a group header actually draws.
 *
 * Syncing writes to the calendar, account and tag tables constantly, and for things
 * nothing renders: ctags, sync tokens, error state, last-sync timestamps. The caches over
 * those tables rebuild on every one of those writes. Comparing this rather than the
 * filters themselves is how they tell a rebuild that changes the task list apart from one
 * that doesn't.
 */
data class FilterAppearance(
    val title: String,
    val icon: String?,
    val tint: Int,
)

fun <K> Map<K, Filter>.appearances(): Map<K, FilterAppearance> =
    mapValues { (_, filter) -> FilterAppearance(filter.title, filter.icon, filter.tint) }
