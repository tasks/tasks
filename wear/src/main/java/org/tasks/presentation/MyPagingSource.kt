package org.tasks.presentation

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyPagingSource<T : Any>(
    private val fetch: suspend (position: Int, limit: Int) -> List<T>?,
) : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val position = params.key ?: 0
        val limit = params.loadSize

        return try {
            val items = withContext (Dispatchers.IO) {
                fetch(position, limit) ?: emptyList()
            }

            LoadResult.Page(
                data = items,
                prevKey = if (position <= 0) null else position - limit,
                nextKey = if (items.isEmpty()) null else position + limit
            )
        } catch (e: Exception) {
            Log.e("MyPagingSource", "${e.message}\n${e.stackTrace}")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int {
        return ((state.anchorPosition ?: 0) - state.config.initialLoadSize / 2).coerceAtLeast(0)
    }
}
