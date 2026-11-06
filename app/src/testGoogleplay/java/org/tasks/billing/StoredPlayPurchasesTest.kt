package org.tasks.billing

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.tasks.R
import org.tasks.billing.PlayPurchaseFixtures.ANNUAL_03
import org.tasks.billing.PlayPurchaseFixtures.ANNUAL_03_CANCELLED
import org.tasks.billing.PlayPurchaseFixtures.MONTHLY_01
import org.tasks.billing.PlayPurchaseFixtures.MONTHLY_03
import org.tasks.preferences.Preferences

@RunWith(RobolectricTestRunner::class)
class StoredPlayPurchasesTest {
    private val preferences =
        Preferences(RuntimeEnvironment.getApplication(), "test_preferences")
    private val store = PreferencesPlayPurchaseStore(preferences)

    @Before
    fun setUp() = preferences.clear()

    @Test
    fun aStoredSubscriptionIsStillPro() {
        val purchases = restored(MONTHLY_01)

        assertTrue(purchases.hasPro)
        assertEquals(1, purchases.state.value.subscription?.subscriptionPrice)
    }

    @Test
    fun theDearerMonthlyWins() {
        val purchases = restored(MONTHLY_01, MONTHLY_03)

        assertEquals(3, purchases.state.value.subscription?.subscriptionPrice)
    }

    @Test
    fun monthlyBeatsAnnual() {
        val purchases = restored(MONTHLY_01, ANNUAL_03)

        assertTrue(purchases.state.value.subscription!!.isMonthly)
    }

    @Test
    fun aCancelledSubscriptionIsStillPro() {
        val purchases = restored(ANNUAL_03_CANCELLED)

        assertTrue(purchases.state.value.subscription!!.isCanceled)
        assertTrue(purchases.state.value.subscription!!.isProSubscription)
        assertTrue(purchases.hasPro)
    }

    @Test
    fun aTamperedPayloadIsNotPro() {
        val purchases = restoredPayloads(
            MONTHLY_01.originalJson.replace("monthly_01", "monthly_03") to MONTHLY_01.signature
        )

        assertFalse(purchases.hasPro)
        assertNull(purchases.state.value.subscription)
    }

    @Test
    fun aPayloadSignedForAnotherPurchaseIsNotPro() {
        val purchases = restoredPayloads(MONTHLY_01.originalJson to MONTHLY_03.signature)

        assertFalse(purchases.hasPro)
        assertNull(purchases.state.value.subscription)
    }

    private fun restored(vararg stored: PlayPurchaseFixture) = restoredPayloads(
        *stored.map { it.originalJson to it.signature }.toTypedArray()
    )

    private fun restoredPayloads(vararg stored: Pair<String, String>) = runBlocking {
        preferences.setStringSet(
            R.string.p_purchases,
            stored
                .map { (json, signature) ->
                    Json.encodeToString(mapOf("zza" to json, "zzb" to signature))
                }
                .toHashSet(),
        )
        PlayPurchases(store, PlaySignatureVerifier()).also { it.load() }
    }
}
