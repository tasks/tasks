package org.tasks.billing

import kotlin.test.Test
import kotlin.test.assertEquals

class ModifySubscriptionTest {

    private val noProvider: EntitlementProvider? = null

    @Test
    fun aStorePurchaseChangesPlanInTheApp() {
        assertEquals(
            ModifySubscription.CHANGE_PLAN,
            EntitlementProvider.PLAY.modifySubscription(),
        )
    }

    @Test
    fun aCardSubscriptionChangesPlanInTheApp() {
        assertEquals(
            ModifySubscription.CHANGE_PLAN,
            EntitlementProvider.STRIPE.modifySubscription(),
        )
    }

    @Test
    fun aSponsorshipIsManagedOnGitHub() {
        assertEquals(
            ModifySubscription.OPEN_SPONSOR_PAGE,
            EntitlementProvider.GITHUB_SPONSOR.modifySubscription(),
        )
    }

    @Test
    fun aSponsorIsNeverSentToThePaywall() {
        assertEquals(
            ModifySubscription.OPEN_SPONSOR_PAGE,
            EntitlementProvider.GITHUB_SPONSOR
                .modifySubscription(unattributed = ModifySubscription.CHANGE_PLAN),
        )
    }

    @Test
    fun anUnattributedSubscriptionHasNothingToModify() {
        assertEquals(ModifySubscription.NONE, noProvider.modifySubscription())
    }

    @Test
    fun anAccountTheServerSaysIsSubscribedCanStillChangePlan() {
        assertEquals(
            ModifySubscription.CHANGE_PLAN,
            noProvider.modifySubscription(unattributed = ModifySubscription.CHANGE_PLAN),
        )
    }
}
