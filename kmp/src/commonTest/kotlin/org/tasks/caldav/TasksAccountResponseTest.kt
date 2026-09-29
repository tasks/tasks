package org.tasks.caldav

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TasksAccountResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(body: String) = json.decodeFromString<TasksAccountResponse>(body)

    @Test
    fun readsACloudSubscription() {
        val response = parse(
            """{"subscriptions":[{"provider":"stripe","expires":1793481600000,""" +
                """"isActive":true,"product":"monthly_03"}]}"""
        )

        assertTrue(response.hasCloudSubscription)
        assertTrue(response.hasAnySubscription)
        assertEquals("monthly_03", response.subscriptions.single().product)
    }

    @Test
    fun aSupporterTierIsASubscriptionThatDoesNotCoverSync() {
        val response = parse(
            """{"supporter":[{"provider":"stripe","product":"annual_25",""" +
                """"formatted_price":"${'$'}25.00","status":"active","expires":1793481600000}]}"""
        )

        assertTrue(response.hasAnySubscription)
        assertFalse(response.hasCloudSubscription)
        assertEquals("${'$'}25.00", response.supporter.single().formattedPrice)
        assertEquals("active", response.supporter.single().status)
    }

    @Test
    fun aCancelledSubscriptionStillRunsToTheEndOfThePeriod() {
        val response = parse(
            """{"subscriptions":[{"provider":"stripe","expires":1793481600000,""" +
                """"isActive":true,"product":"monthly_03","cancel_at_period_end":true}]}"""
        )

        assertTrue(response.hasCloudSubscription)
        assertTrue(response.subscriptions.single().cancelAtPeriodEnd)
    }

    @Test
    fun aSubscriptionThatRenewsSaysSoByOmission() {
        val response = parse(
            """{"subscriptions":[{"provider":"stripe","isActive":true,"product":"annual_30"}]}"""
        )

        assertFalse(response.subscriptions.single().cancelAtPeriodEnd)
    }

    @Test
    fun anAccountWithNothingHasNothing() {
        val response = parse("""{"createdAt":1,"guest":false}""")

        assertFalse(response.hasAnySubscription)
        assertFalse(response.hasCloudSubscription)
    }

    @Test
    fun theLegacySubscriptionFieldStillCounts() {
        val response = parse("""{"subscription":{"free":false,"provider":"stripe"}}""")

        assertTrue(response.hasAnySubscription)
    }
}
