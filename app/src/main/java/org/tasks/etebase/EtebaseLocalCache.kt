package org.tasks.etebase

import android.content.Context
import com.etebase.client.*
import com.etebase.client.Collection
import com.etebase.client.exceptions.EtebaseException
import com.etebase.client.exceptions.UrlParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*

class EtebaseLocalCache private constructor(context: Context, username: String) {
    private val fsCache: FileSystemCache = FileSystemCache.create(context.filesDir.absolutePath, username)

    private suspend fun clearUserCache() {
        withContext(Dispatchers.IO) {
            fsCache.clearUserCache()
        }
    }

    suspend fun collectionList(colMgr: CollectionManager): List<Collection> =
            withContext(Dispatchers.IO) {
                fsCache._unstable_collectionList(colMgr).filter { !it.isDeleted }
            }

    suspend fun collectionGet(colMgr: CollectionManager, colUid: String): Collection =
            withContext(Dispatchers.IO) {
                fsCache.collectionGet(colMgr, colUid)
            }

    suspend fun collectionSet(colMgr: CollectionManager, collection: Collection) {
        if (collection.isDeleted) {
            collectionUnset(colMgr, collection.uid)
        } else {
            withContext(Dispatchers.IO) {
                fsCache.collectionSet(colMgr, collection)
            }
        }
    }

    suspend fun collectionUnset(colMgr: CollectionManager, collection: RemovedCollection) {
        collectionUnset(colMgr, collection.uid())
    }

    private suspend fun collectionUnset(colMgr: CollectionManager, colUid: String) {
        withContext(Dispatchers.IO) {
            try {
                fsCache.collectionUnset(colMgr, colUid)
            } catch (e: UrlParseException) {
                // Ignore, as it just means the file doesn't exist
            }
        }
    }

    suspend fun itemGet(itemMgr: ItemManager, colUid: String, itemUid: String): Item? =
            withContext(Dispatchers.IO) {
        // Need the try because the inner call doesn't return null on missing, but an error
        try {
            fsCache.itemGet(itemMgr, colUid, itemUid)
        } catch (e: EtebaseException) {
            null
        }
    }

    suspend fun itemSet(itemMgr: ItemManager, colUid: String, item: Item) {
        withContext(Dispatchers.IO) {
            if (item.isDeleted) {
                fsCache.itemUnset(itemMgr, colUid, item.uid)
            } else {
                fsCache.itemSet(itemMgr, colUid, item)
            }
        }
    }

    companion object {
        private val localCacheCache: HashMap<String, EtebaseLocalCache> = HashMap()

        fun getInstance(context: Context, username: String): EtebaseLocalCache {
            synchronized(localCacheCache) {
                val cached = localCacheCache[username]
                return if (cached != null) {
                    cached
                } else {
                    val ret = EtebaseLocalCache(context, username)
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