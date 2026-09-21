package org.tasks.googleapis

import org.tasks.filters.CaldavFilter

interface DefaultListProvider {
    suspend fun getDefaultList(): CaldavFilter
    suspend fun clearDefaultList()
}
