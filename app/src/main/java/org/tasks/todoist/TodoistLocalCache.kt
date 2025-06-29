package org.tasks.todoist

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.tasks.todoist.TodoistClient.TodoistCollection
import org.tasks.todoist.TodoistClient.TodoistItem

class TodoistLocalCache private constructor(context: Context, username: String) {
    suspend fun clearUserCache() {
        // Stubbed implementation for Todoist API
    }

    suspend fun saveStoken(stoken: String) {
        // Stubbed implementation for Todoist API
    }

    suspend fun loadStoken(): String? = withContext(Dispatchers.IO) {
        // Stubbed implementation for Todoist API
        null
    }

    suspend fun collectionList(colMgr: Any): List<TodoistCollection> =
            withContext(Dispatchers.IO) {
                // Stubbed implementation for Todoist API
                emptyList()
            }

    suspend fun collectionGet(colMgr: Any, colUid: String): TodoistCollection =
            withContext(Dispatchers.IO) {
                // Stubbed implementation for Todoist API
                TodoistCollection()
            }

    suspend fun collectionSet(colMgr: Any, collection: TodoistCollection) {
        // Stubbed implementation for Todoist API
    }

    suspend fun collectionUnset(colMgr: Any, collection: Any) {
        // Stubbed implementation for Todoist API
    }

    private suspend fun collectionUnset(colMgr: Any, colUid: String) {
        // Stubbed implementation for Todoist API
    }

    suspend fun itemGet(itemMgr: Any, colUid: String, itemUid: String): TodoistItem? =
            withContext(Dispatchers.IO) {
                // Stubbed implementation for Todoist API
                null
            }

    suspend fun itemSet(itemMgr: Any, colUid: String, item: TodoistItem) {
        // Stubbed implementation for Todoist API
    }

    companion object {
        private val localCacheCache: HashMap<String, TodoistLocalCache> = HashMap()

        fun getInstance(context: Context, username: String): TodoistLocalCache {
            synchronized(localCacheCache) {
                val cached = localCacheCache[username]
                return if (cached != null) {
                    cached
                } else {
                    val ret = TodoistLocalCache(context, username)
                    localCacheCache[username] = ret
                    ret
                }
            }
        }

        fun clear(context: Context) = runBlocking {
            val users = synchronized(localCacheCache) {
                localCacheCache.keys.toList()
            }
            users.forEach { clear(context, it) }
        }

        suspend fun clear(context: Context, username: String) {
            val localCache = getInstance(context, username)
            localCache.clearUserCache()
            localCacheCache.remove(username)
        }
    }
}
