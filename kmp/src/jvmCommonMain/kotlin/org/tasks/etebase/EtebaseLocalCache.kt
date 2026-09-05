package org.tasks.etebase

import com.etebase.client.*
import com.etebase.client.Collection
import com.etebase.client.exceptions.EtebaseException
import com.etebase.client.exceptions.UrlParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*

class EtebaseLocalCache private constructor(filesDir: String, username: String) {
    private val fsCache: FileSystemCache = FileSystemCache.create(filesDir, username)

    private suspend fun clearUserCache() {
        withContext(Dispatchers.IO) {
            fsCache.clearUserCache()
        }
    }

    suspend fun saveStoken(stoken: String) {
        withContext(Dispatchers.IO) {
            fsCache.saveStoken(stoken)
        }
    }

    suspend fun loadStoken(): String? = withContext(Dispatchers.IO) {
        fsCache.loadStoken()
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
                try {
                    fsCache.itemUnset(itemMgr, colUid, item.uid)
                } catch (e: UrlParseException) {
                    // Ignore, as it just means the file doesn't exist
                }
            } else {
                fsCache.itemSet(itemMgr, colUid, item)
            }
        }
    }

    companion object {
        private val localCacheCache: HashMap<Pair<String, String>, EtebaseLocalCache> = HashMap()

        fun getInstance(filesDir: String, username: String): EtebaseLocalCache {
            synchronized(localCacheCache) {
                val key = EtebaseCacheLocation.key(filesDir, username)
                val cached = localCacheCache[key]
                return if (cached != null) {
                    cached
                } else {
                    val ret = EtebaseLocalCache(filesDir, username)
                    localCacheCache[key] = ret
                    ret
                }
            }
        }

        fun clear(filesDir: String) = runBlocking {
            val users = synchronized(localCacheCache) {
                localCacheCache.keys.toList()
            }
            users.filter { (root, _) ->
                root == java.io.File(filesDir).absolutePath ||
                    root.startsWith(java.io.File(filesDir, "etebase-accounts").absolutePath + java.io.File.separator)
            }.forEach { (root, username) -> clear(root, username) }
        }

        suspend fun clear(filesDir: String, username: String) {
            val localCache = getInstance(filesDir, username)
            localCache.clearUserCache()
            synchronized(localCacheCache) {
                localCacheCache.remove(EtebaseCacheLocation.key(filesDir, username))
            }
        }
    }
}
