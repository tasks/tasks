package org.tasks.billing

import com.android.billingclient.api.Purchase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.tasks.R
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

class PreferencesPlayPurchaseStore @Inject constructor(
    private val preferences: Preferences,
) : PlayPurchaseStore {

    override suspend fun load(): List<PlayPurchase> = try {
        preferences.getStringSet(R.string.p_purchases).mapNotNull { it.toPlayPurchaseOrNull() }
    } catch (e: Exception) {
        Timber.e(e)
        emptyList()
    }

    override suspend fun save(purchases: List<PlayPurchase>) {
        try {
            preferences.setStringSet(
                R.string.p_purchases,
                purchases.map { it.toStoredJson() }.toHashSet(),
            )
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun String.toPlayPurchaseOrNull(): PlayPurchase? = try {
        Json.parseToJsonElement(this).jsonObject.let {
            Purchase(
                it[FIELD_JSON]!!.jsonPrimitive.content,
                it[FIELD_SIGNATURE]!!.jsonPrimitive.content,
            ).toPlayPurchase()
        }
    } catch (e: Exception) {
        Timber.e(e)
        null
    }

    private fun PlayPurchase.toStoredJson(): String =
        Json.encodeToString(mapOf(FIELD_JSON to originalJson, FIELD_SIGNATURE to signature))

    companion object {
        private const val FIELD_JSON = "zza"
        private const val FIELD_SIGNATURE = "zzb"
    }
}
