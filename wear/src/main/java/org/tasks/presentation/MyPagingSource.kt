package org.tasks.presentation

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadParams.Append
import androidx.paging.PagingSource.LoadParams.Prepend
import androidx.paging.PagingSource.LoadParams.Refresh
import androidx.paging.PagingState
import timber.log.Timber

private const val INITIAL_ITEM_COUNT = -1

class MyPagingSource<T : Any>(
    private val fetch: suspend (position: Int, limit: Int) -> Pair<Int, List<T>>,
) : PagingSource<Int, T>() {

    private var itemCount = INITIAL_ITEM_COUNT

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        return try {
            val key = params.key ?: 0
            val limit = getLimit(params, key)
            val offset = getOffset(params, key, itemCount)
            val (newCount, data) = fetch(offset, limit)
            if (itemCount == INITIAL_ITEM_COUNT) {
                itemCount = newCount
            }
            val nextPosToLoad = offset + data.size
            val nextKey =
                if (data.isEmpty() || data.size < limit || nextPosToLoad >= itemCount) {
                    null
                } else {
                    nextPosToLoad
                }
            val prevKey = if (offset <= 0 || data.isEmpty()) null else offset
            LoadResult.Page(
                data = data,
                prevKey = prevKey,
                nextKey = nextKey,
                itemsBefore = offset,
                itemsAfter = maxOf(0, itemCount - nextPosToLoad)
            )
        } catch (e: Exception) {
            Timber.e(e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? = state.getClippedRefreshKey()
}

private fun <Value : Any> PagingState<Int, Value>.getClippedRefreshKey(): Int? {
    return when (val anchorPosition = anchorPosition) {
        null -> null
        else -> maxOf(0, anchorPosition - (config.initialLoadSize / 2))
    }
}

fun getLimit(params: LoadParams<Int>, key: Int): Int {
    return when (params) {
        is Prepend ->
            if (key < params.loadSize) {
                key
            } else {
                params.loadSize
            }
        else -> params.loadSize
    }
}

fun getOffset(params: LoadParams<Int>, key: Int, itemCount: Int): Int {
    return when (params) {
        is Prepend ->
            if (key < params.loadSize) {
                0
            } else {
                key - params.loadSize
            }
        is Append -> key
        is Refresh ->
            if (itemCount != INITIAL_ITEM_COUNT && key >= itemCount) {
                maxOf(0, itemCount - params.loadSize)
            } else {
                key
            }
    }
}
