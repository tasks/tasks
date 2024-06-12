package org.tasks.filters

@Deprecated("Use manual ordering")
interface AstridOrderingFilter : Filter {
    var filterOverride: String?

    fun getSqlQuery(): String = filterOverride ?: sql!!
}
