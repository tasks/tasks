package org.tasks.filters

@Deprecated("Use manual ordering")
abstract class AstridOrderingFilter : Filter() {
    abstract var filterOverride: String?

    fun getSqlQuery(): String = filterOverride ?: sql!!
}
