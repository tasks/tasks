package org.tasks.billing

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.tasks.R
import org.tasks.billing.PlayPurchaseFixtures.ANNUAL_03
import org.tasks.billing.PlayPurchaseFixtures.ANNUAL_03_CANCELLED
import org.tasks.billing.PlayPurchaseFixtures.MONTHLY_03
import org.tasks.preferences.Preferences

@RunWith(RobolectricTestRunner::class)
class PreferencesPlayPurchaseStoreTest {
    private val preferences =
        Preferences(RuntimeEnvironment.getApplication(), "test_preferences")
    private val store = PreferencesPlayPurchaseStore(preferences)

    @Before
    fun setUp() = preferences.clear()

    @Test
    fun theSubscriptionAnEarlierReleaseStoredIsStillRead() = runBlocking {
        store(stored(ANNUAL_03))

        val purchase = store.load().single()

        assertEquals("annual_03", purchase.sku)
        assertEquals(ANNUAL_03.purchaseToken, purchase.purchaseToken)
        assertEquals(ANNUAL_03.originalJson, purchase.originalJson)
        assertEquals(ANNUAL_03.signature, purchase.signature)
        assertTrue(purchase.isPurchased)
        assertTrue(purchase.isAcknowledged)
        assertTrue(purchase.isAutoRenewing)
    }

    @Test
    fun aCancelledSubscriptionComesBackCancelled() = runBlocking {
        store(stored(ANNUAL_03_CANCELLED))

        assertFalse(store.load().single().isAutoRenewing)
    }

    @Test
    fun everyStoredPurchaseIsRead() = runBlocking {
        store(stored(ANNUAL_03), stored(MONTHLY_03))

        assertEquals(setOf("annual_03", "monthly_03"), store.load().map { it.sku }.toSet())
    }

    @Test
    fun whatIsSavedIsReadBackUnchanged() = runBlocking {
        store(stored(ANNUAL_03))
        val purchase = store.load().single()
        preferences.clear()

        store.save(listOf(purchase))

        assertEquals(purchase, store.load().single())
    }

    @Test
    fun whatIsSavedKeepsTheKeysTheEarlierReleaseWrote() = runBlocking {
        store(stored(ANNUAL_03))
        val purchase = store.load().single()
        preferences.clear()

        store.save(listOf(purchase))

        val written = preferences.getStringSet(R.string.p_purchases).single()
        assertEquals(
            setOf("zza", "zzb"),
            (Json.parseToJsonElement(written) as JsonObject).keys,
        )
    }

    @Test
    fun aPurchaseThatCannotBeReadIsSkippedWithoutLosingTheRest() = runBlocking {
        store(
            stored(ANNUAL_03),
            "not json at all",
            """{"zza":"{}"}""",
        )

        assertEquals(listOf("annual_03"), store.load().map { it.sku })
    }

    @Test
    fun nothingStoredIsNoPurchases() = runBlocking {
        assertEquals(emptyList<PlayPurchase>(), store.load())
    }

    private fun store(vararg purchases: String) =
        preferences.setStringSet(R.string.p_purchases, purchases.toHashSet())

    private fun stored(fixture: PlayPurchaseFixture) = Json.encodeToString(
        mapOf("zza" to fixture.originalJson, "zzb" to fixture.signature)
    )
}
