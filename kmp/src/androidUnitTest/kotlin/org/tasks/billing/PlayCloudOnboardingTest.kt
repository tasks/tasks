package org.tasks.billing

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.tasks.InMemoryDataStore
import org.tasks.analytics.AnalyticsEvents
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences

class PlayCloudOnboardingTest {
    private var accounts = emptyList<CaldavAccount>()
    private val steps = mutableListOf<String>()
    private val tasksPreferences = TasksPreferences(InMemoryDataStore())
    private val caldavDao = mock<CaldavDao> {
        onBlocking { getAccounts(any<List<Int>>()) } doAnswer { accounts }
    }

    @Test
    fun buyingCloudWithNoAccountStartsTheFunnel() = runTest {
        onPurchased(purchase("annual_30"))

        assertTrue(needsCloudOnboarding())
        assertEquals(listOf(AnalyticsEvents.CloudOnboarding.TRIGGERED), steps)
    }

    @Test
    fun buyingSupportWithoutCloudStartsNothing() = runTest {
        onPurchased(purchase("annual_01"))

        assertFalse(needsCloudOnboarding())
        assertTrue(steps.isEmpty())
    }

    @Test
    fun anUpgradeThatReachesCloudStartsTheFunnel() = runTest {
        onPurchased(purchase("annual_01"), purchase("annual_30"))

        assertTrue(needsCloudOnboarding())
    }

    @Test
    fun aThemesPurchaseStartsNothing() = runTest {
        onPurchased(purchase(PlayPurchase.SKU_THEMES))

        assertFalse(needsCloudOnboarding())
    }

    @Test
    fun someoneAlreadySignedInNeedsNoFunnel() = runTest {
        accounts = listOf(CaldavAccount(accountType = CaldavAccount.TYPE_TASKS))

        onPurchased(purchase("annual_30"))

        assertFalse(needsCloudOnboarding())
        assertTrue(steps.isEmpty())
    }

    @Test
    fun aPendingPurchaseStartsNothing() = runTest {
        onPurchased(purchase("annual_30").copy(isPurchased = false))

        assertFalse(needsCloudOnboarding())
        assertTrue(steps.isEmpty())
    }

    @Test
    fun aPurchaseThatFailedStartsNothing() = runTest {
        onPurchased()

        assertFalse(needsCloudOnboarding())
        assertTrue(steps.isEmpty())
    }

    private suspend fun onPurchased(vararg purchased: PlayPurchase) =
        maybeTriggerCloudOnboarding(
            purchased = purchased.toList(),
            caldavDao = caldavDao,
            tasksPreferences = tasksPreferences,
            logStep = { steps.add(it) },
        )

    private suspend fun needsCloudOnboarding() =
        tasksPreferences.get(TasksPreferences.needsCloudOnboarding, false)

    private fun purchase(sku: String) = PlayPurchase(
        sku = sku,
        purchaseToken = "token-$sku",
        originalJson = "",
        signature = "",
        isPurchased = true,
        isAcknowledged = true,
        isAutoRenewing = true,
    )
}
