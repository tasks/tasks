package org.tasks.etebase

import android.content.Context
import com.etebase.client.*
import com.etebase.client.Collection
import com.etesync.journalmanager.Exceptions
import com.etesync.journalmanager.Exceptions.IntegrityException
import com.etesync.journalmanager.Exceptions.VersionTooNewException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.time.DateTimeUtils.currentTimeMillis
import timber.log.Timber
import java.io.IOException

class EtebaseClient(
        private val context: Context,
        private val username: String,
        private val etebase: Account,
        private val caldavDao: CaldavDao
) {
    private val cache = EtebaseLocalCache.getInstance(context, username)

    @Throws(IOException::class, Exceptions.HttpException::class)
    fun getSession(): String = etebase.save(null)

    @Throws(Exceptions.HttpException::class)
    suspend fun getCollections(): List<Collection> {
        val collectionManager = etebase.collectionManager
        do {
            val response = withContext(Dispatchers.IO) {
                collectionManager.list(TYPE_TASKS)
            }
            response.data.forEach {
                cache.collectionSet(collectionManager, it)
            }
            response.removedMemberships.forEach {
                cache.collectionUnset(collectionManager, it)
            }
        } while (!response.isDone)
        return cache.collectionList(collectionManager)
    }

    @Throws(IntegrityException::class, Exceptions.HttpException::class, VersionTooNewException::class)
    suspend fun fetchItems(
            collection: Collection,
            calendar: CaldavCalendar,
            callback: suspend (Pair<String?, List<Item>>) -> Unit
    ) {
        val itemManager = etebase.collectionManager.getItemManager(collection)
        var stoken = calendar.ctag
        do {
            val items = withContext(Dispatchers.IO) {
                itemManager.list(FetchOptions().stoken(stoken).limit(MAX_FETCH))
            }
            stoken = items.stoken
            callback(Pair(stoken, items.data.toList()))
        } while (!items.isDone)
    }

    suspend fun updateItem(collection: Collection, task: CaldavTask, content: ByteArray): Item {
        val itemManager = etebase.collectionManager.getItemManager(collection)
        val item = cache.itemGet(itemManager, collection.uid, task.`object`!!)
                ?: itemManager
                        .create(ItemMetadata().apply { name = task.remoteId!! }, "")
                        .apply {
                            task.`object` = uid
                            caldavDao.update(task)
                        }
        item.meta = updateMtime(item.meta)
        item.content = content
        return item
    }

    suspend fun deleteItem(collection: Collection, task: CaldavTask): Item? {
        val itemManager = etebase.collectionManager.getItemManager(collection)
        return cache.itemGet(itemManager, collection.uid, task.`object`!!)
                ?.takeIf { !it.isDeleted }
                ?.apply {
                    meta = updateMtime(meta)
                    delete()
                }
    }

    private fun updateMtime(meta: ItemMetadata): ItemMetadata {
        meta.mtime = currentTimeMillis()
        return meta
    }

    suspend fun updateCache(collection: Collection, items: List<Item>) {
        val itemManager = etebase.collectionManager.getItemManager(collection)
        items.forEach { cache.itemSet(itemManager, collection.uid, it) }
    }

    suspend fun uploadChanges(collection: Collection, items: List<Item>) {
        val itemManager = etebase.collectionManager.getItemManager(collection)
        withContext(Dispatchers.IO) {
            itemManager.batch(items.toTypedArray())
        }
    }

    suspend fun logout() {
        try {
            EtebaseLocalCache.clear(context, username)
            withContext(Dispatchers.IO) {
                etebase.logout()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    @Throws(VersionTooNewException::class, IntegrityException::class, Exceptions.HttpException::class)
    suspend fun makeCollection(name: String, color: Int) =
            etebase
                    .collectionManager
                    .create(TYPE_TASKS, ItemMetadata(), "")
                    .let { setAndUpload(it, name, color) }

    @Throws(VersionTooNewException::class, IntegrityException::class, Exceptions.HttpException::class)
    suspend fun updateCollection(calendar: CaldavCalendar, name: String, color: Int) =
            cache
                    .collectionGet(etebase.collectionManager, calendar.url!!)
                    .let { setAndUpload(it, name, color) }

    @Throws(Exceptions.HttpException::class)
    suspend fun deleteCollection(calendar: CaldavCalendar) =
            cache
                    .collectionGet(etebase.collectionManager, calendar.url!!)
                    .apply { delete() }
                    .let { setAndUpload(it) }

    private suspend fun setAndUpload(
            collection: Collection,
            name: String? = null,
            color: Int? = null
    ): String {
        collection.meta = collection.meta.let { meta ->
            name?.let { meta.name = it }
            color?.let { meta.color = it.toHexColor() }
            meta.mtime = currentTimeMillis()
            meta
        }
        val collectionManager = etebase.collectionManager
        withContext(Dispatchers.IO) {
            collectionManager.upload(collection)
        }
        cache.collectionSet(collectionManager, collection)
        return collection.uid
    }

    companion object {
        private const val TYPE_TASKS = "etebase.vtodo"
        private const val MAX_FETCH = 30L

        private fun Int.toHexColor(): String? = takeIf { this != 0 }?.let {
            java.lang.String.format("#%06X", 0xFFFFFF and it)
        }
    }
}