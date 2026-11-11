package org.tasks.compose.pricing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.billing.BillingCatalog
import org.tasks.billing.BillingOption
import org.tasks.billing.UnlockSource
import org.tasks.themes.TestTags

@OptIn(ExperimentalTestApi::class)
class CardPricingTest {

    private val annual = BillingOption(
        priceId = "pri_cloud_year",
        sku = "cloud_annual",
        amountCents = 2999,
        entitles = true,
        trialDays = 7,
    )

    private val monthly = BillingOption(
        priceId = "pri_cloud_month",
        sku = "cloud_monthly",
        amountCents = 299,
        interval = BillingOption.INTERVAL_MONTH,
        entitles = true,
        trialDays = 7,
    )

    private val supporter = BillingOption(
        priceId = "pri_support_year",
        sku = "annual_01",
        amountCents = 100,
    )

    private val everyTier = StoreBilling(
        catalog = BillingCatalog.from(listOf(annual, monthly, supporter)),
        onSelect = {},
    )

    @Test
    fun theCtaQuotesThePriceFromTheCatalog() = runComposeUiTest {
        showPaywall(StoreBilling(catalog = BillingCatalog.from(listOf(annual)), onSelect = {}))

        assertShowing("Subscribe for \$29.99/year")
    }

    @Test
    fun aFreeWeekIsNotPromisedToSomebodyWhoHasBoughtBefore() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                knownCustomer = true,
                onSelect = {},
            )
        )

        assertShowing("Subscribe for \$29.99/year")
        assertNotShowing("7-day trial")
    }

    @Test
    fun theGooglePlayCtaIsLeftToBuildsWithoutACardStore() = runComposeUiTest {
        showPaywall(store = null)

        assertShowing("Subscribe on Google Play")
    }

    @Test
    fun aCatalogStillLoadingDoesNotQuoteAPriceItHasNotFetched() = runComposeUiTest {
        showPaywall(StoreBilling(loadingPrices = true, onSelect = {}))

        onAllNodesWithTag(TestTags.PRICE_PLACEHOLDER).fetchSemanticsNodes().let {
            assertTrue("expected the price to be a placeholder", it.isNotEmpty())
        }
        assertNotShowing("\$29.99")
        assertNotShowing("\$2.50")
    }

    @Test
    fun theSupporterCardOnlyShimmersWhereAPriceWillLoad() = runComposeUiTest {
        showPaywall(StoreBilling(loadingPrices = true, onSelect = {}), mode = PricingMode.NYP_ONLY)

        assertEquals(1, onAllNodesWithTag(TestTags.PRICE_PLACEHOLDER).fetchSemanticsNodes().size)
    }

    @Test
    fun theCloudCardShimmersForItsPriceAndItsBillingNote() = runComposeUiTest {
        showPaywall(StoreBilling(loadingPrices = true, onSelect = {}), mode = PricingMode.CLOUD_ONLY)

        assertEquals(2, onAllNodesWithTag(TestTags.PRICE_PLACEHOLDER).fetchSemanticsNodes().size)
    }

    @Test
    fun aStoreThatCannotPriceAnythingSaysNothingRatherThanDollars() = runComposeUiTest {
        showPaywall(StoreBilling(onSelect = {}))

        assertNotShowing("\$29.99")
        onAllNodesWithTag(TestTags.PRICE_PLACEHOLDER).fetchSemanticsNodes().let {
            assertTrue("expected no shimmer once the catalog has settled", it.isEmpty())
        }
    }

    @Test
    fun buildsWithoutACardStoreKeepTheirIndicativePrice() = runComposeUiTest {
        showPaywall(store = null)

        assertShowing("\$29.99")
        onAllNodesWithTag(TestTags.PRICE_PLACEHOLDER).fetchSemanticsNodes().let {
            assertTrue("expected no shimmer where no catalog is ever fetched", it.isEmpty())
        }
    }

    @Test
    fun aSettledCatalogWithNothingToSellSaysSoInsteadOfOfferingGooglePlay() = runComposeUiTest {
        showPaywall(StoreBilling(onSelect = {}))

        assertShowing("Billing isn't available right now. Please try again later.")
        assertTrue(
            "expected no Google Play call to action",
            onAllNodes(hasText("Subscribe on Google Play")).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun aPurchaseInFlightDisablesTheCta() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                busySku = annual.sku,
                onSelect = {},
            )
        )

        onNodeWithText("Annual").assertExists()
        onAllNodes(hasText("Subscribe for \$29.99/year")).fetchSemanticsNodes().let {
            assertTrue("expected the priced cta to be replaced by a spinner", it.isEmpty())
        }
    }

    @Test
    fun theCurrentPlanCannotBeBoughtAgain() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                currentSku = annual.sku,
                onSelect = {},
            )
        )

        onNodeWithText("Subscribed").assertIsNotEnabled()
    }

    @Test
    fun restoreIsOnThePaywallAndNotOnlyAfterAReinstall() = runComposeUiTest {
        var restored = false
        showPaywall(
            store = StoreBilling(catalog = BillingCatalog.from(listOf(annual)), onSelect = {}),
            onRestorePurchases = { restored = true },
        )

        onNodeWithText("Restore purchases").performScrollTo().performClick()

        assertTrue("expected the restore screen to be asked for", restored)
    }

    @Test
    fun anUpsellMovesSignInToTheTopBar() {
        listOf(PricingMode.BOTH, PricingMode.NYP_ONLY).forEach { mode ->
            runComposeUiTest {
                var signedIn = false
                showPaywall(
                    store = StoreBilling(catalog = BillingCatalog.from(listOf(annual, supporter)), onSelect = {}),
                    mode = mode,
                    onSignIn = { signedIn = true },
                )

                assertNotShowing("Already a subscriber or invited guest?")
                assertEquals("$mode", 1, onAllNodes(hasText("Sign in")).fetchSemanticsNodes().size)
                assertTrue(
                    "$mode: expected sign in at the top",
                    onNodeWithText("Sign in").getBoundsInRoot().bottom <
                            onNodeWithText("Hi! My name is Alex", substring = true).getBoundsInRoot().top,
                )
                onNodeWithText("Sign in").performClick()
                assertTrue("$mode: expected sign in to be asked for", signedIn)
            }
        }
    }

    @Test
    fun everyPaywallOpensWithANoteFromTheDeveloper() {
        PricingMode.entries.forEach { mode ->
            runComposeUiTest {
                showPaywall(
                    store = StoreBilling(catalog = BillingCatalog.from(listOf(annual, supporter)), onSelect = {}),
                    mode = mode,
                )

                assertTrue(
                    "$mode: expected the note above the plans",
                    onNodeWithText("Hi! My name is Alex", substring = true).fetchSemanticsNode().positionInRoot.y <
                            onAllNodes(hasText("Tasks.org Cloud") or hasText("Tasks.org Supporter"))
                                .fetchSemanticsNodes()
                                .minOf { it.positionInRoot.y },
                )
                assertNotShowing("Free and open source")
            }
        }
    }

    @Test
    fun addingACloudAccountKeepsTheSignInFooter() = runComposeUiTest {
        showPaywall(StoreBilling(catalog = BillingCatalog.from(listOf(annual)), onSelect = {}))

        assertShowing("Already a subscriber or invited guest?")
        assertEquals(1, onAllNodes(hasText("Sign in")).fetchSemanticsNodes().size)
    }

    @Test
    fun aSignedInUserIsNotAskedToSignIn() = runComposeUiTest {
        showPaywall(
            store = StoreBilling(catalog = BillingCatalog.from(listOf(annual, supporter)), onSelect = {}),
            mode = PricingMode.BOTH,
            showSignIn = false,
        )

        assertNotShowing("Sign in")
    }

    @Test
    fun theRestoreLinkDoesNotAskAgainWhatTheSignInFooterAsked() = runComposeUiTest {
        showPaywall(StoreBilling(catalog = BillingCatalog.from(listOf(annual)), onSelect = {}))

        assertShowing("Already a subscriber or invited guest?")
        assertShowing("Restore purchases")
        assertTrue(
            "expected the restore link not to repeat the question",
            onAllNodes(hasText("Already a supporter?")).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun restoreIsOfferedOnceAtTheBottomOfEveryPaywall() {
        PricingMode.entries.forEach { mode ->
            runComposeUiTest {
                showPaywall(
                    store = StoreBilling(catalog = BillingCatalog.from(listOf(annual, supporter)), onSelect = {}),
                    mode = mode,
                )

                assertEquals("$mode", 1, onAllNodes(hasText("Restore purchases")).fetchSemanticsNodes().size)
                assertNotShowing("Already a supporter?")
                assertTrue(
                    "$mode: expected restore below the plans",
                    onNodeWithText("Restore purchases").fetchSemanticsNode().positionInRoot.y >
                            onAllNodes(hasText("Subscribe for", substring = true)).fetchSemanticsNodes()
                                .maxOf { it.positionInRoot.y + it.size.height },
                )
            }
        }
    }

    @Test
    fun anAccountAnotherProviderPaysForIsWarnedButStillSoldTo() = runComposeUiTest {
        var selected: BillingOption? = null
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                coveredElsewhere = true,
                coveredBy = UnlockSource.GOOGLE_PLAY,
                onSelect = { selected = it },
            )
        )

        assertShowing("You already have an active Google Play subscription.")
        onNodeWithText("Subscribe for \$29.99/year").performScrollTo().performClick()

        assertEquals(annual.sku, selected?.sku)
    }

    @Test
    fun aGitHubSponsorIsWarnedByName() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                coveredElsewhere = true,
                coveredBy = UnlockSource.GITHUB,
                onSelect = {},
            )
        )

        assertShowing("You already have an active GitHub sponsorship.")
    }

    @Test
    fun aCardSubscriptionOfTheirOwnIsStillTheirsToReprice() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual, monthly)),
                currentSku = monthly.sku,
                coveredElsewhere = true,
                onSelect = {},
            )
        )

        assertShowing("Change to \$29.99/year")
        assertTrue(
            "expected no coverage warning on a reprice",
            onAllNodes(hasText("active subscription somewhere else", substring = true))
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun aSupporterSubscriptionDoesNotSuppressTheCoverageWarning() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual, supporter)),
                currentSku = supporter.sku,
                coveredElsewhere = true,
                coveredBy = UnlockSource.STRIPE,
                onSelect = {},
            )
        )

        assertShowing("You already have an active subscription paid by card.")
    }

    @Test
    fun aCloudPlanTheStoreNoLongerSellsStillSuppressesTheWarning() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                currentSku = "monthly_499",
                coveredElsewhere = true,
                coveredBy = UnlockSource.STRIPE,
                onSelect = {},
            )
        )

        assertNotShowing("You already have an active subscription paid by card.")
    }

    @Test
    fun aProviderTheAppCannotNameIsWarnedAboutInTheGeneralCase() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual)),
                coveredElsewhere = true,
                onSelect = {},
            )
        )

        assertShowing("You already have an active subscription somewhere else.")
        assertShowing("Subscribe for \$29.99/year")
    }

    @Test
    fun aPaymentElsewhereIsWarnedAboutOnceAboveBothTiers() = runComposeUiTest {
        showPaywall(
            store = StoreBilling(
                catalog = BillingCatalog.from(listOf(annual, supporter)),
                coveredElsewhere = true,
                coveredBy = UnlockSource.STRIPE,
                onSelect = {},
            ),
            mode = PricingMode.BOTH,
        )

        assertEquals(
            1,
            onAllNodes(hasText("You already have an active subscription paid by card."))
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aNewCustomerSeesTheTrialAsTheLastCloudFeature() = runComposeUiTest {
        showPaywall(StoreBilling(catalog = BillingCatalog.from(listOf(annual)), onSelect = {}))

        val trial = onNodeWithText("7-day free trial for new subscribers").getBoundsInRoot()
        assertTrue(
            "expected the trial after the other features",
            onNodeWithText("Email-to-task with a private inbox").getBoundsInRoot().bottom <= trial.top,
        )
        assertTrue(
            "expected the trial above the subscribe button",
            trial.bottom < onNodeWithText("Subscribe for \$29.99/year").getBoundsInRoot().top,
        )
    }

    @Test
    fun theSupporterFeaturesListTheTrialToo() = runComposeUiTest {
        showPaywall(
            store = StoreBilling(
                catalog = BillingCatalog.from(listOf(supporter.copy(trialDays = 7))),
                onSelect = {},
            ),
            mode = PricingMode.NYP_ONLY,
        )

        assertTrue(
            "expected the trial after the other supporter features",
            onNodeWithText("Support open source software").getBoundsInRoot().bottom <=
                    onNodeWithText("7-day free trial for new subscribers").getBoundsInRoot().top,
        )
    }

    @Test
    fun theFDroidSupporterCardDoesNotOfferWhatIsAlreadyUnlocked() = runComposeUiTest {
        showPaywall(
            store = StoreBilling(
                catalog = BillingCatalog.from(listOf(supporter.copy(trialDays = 7))),
                onSelect = {},
            ),
            mode = PricingMode.NYP_ONLY,
            supporterFeatures = SupporterFeatures.ANDROID_CARD,
        )

        assertNotShowing("7-day free trial")
        assertNotShowing("Unlocks all in-app features")
        assertNotShowing("Choose your own subscription price")
    }

    @Test
    fun aPriceWithoutATrialDoesNotMentionOne() = runComposeUiTest {
        showPaywall(
            StoreBilling(
                catalog = BillingCatalog.from(listOf(annual.copy(trialDays = 0))),
                onSelect = {},
            )
        )

        assertNotShowing("7-day trial")
    }

    @Test
    fun billingTroubleIsReportedOnceAboveTheTiers() = runComposeUiTest {
        showPaywall(
            store = StoreBilling(catalog = BillingCatalog(), onSelect = {}),
            mode = PricingMode.BOTH,
        )

        val notice = "Billing isn't available right now. Please try again later."
        assertEquals(1, onAllNodes(hasText(notice)).fetchSemanticsNodes().size)
        assertTrue(
            "expected the notice above the cloud card",
            onNodeWithText(notice).getBoundsInRoot().bottom <
                    onNodeWithText("Tasks.org Cloud").getBoundsInRoot().top,
        )
    }

    @Test
    fun aSupporterRepricingTheirOwnPlanIsOnlyWarnedOnCloud() = runComposeUiTest {
        showPaywall(
            store = StoreBilling(
                catalog = BillingCatalog.from(listOf(annual, supporter)),
                currentSku = supporter.sku,
                coveredElsewhere = true,
                onSelect = {},
            ),
            mode = PricingMode.BOTH,
        )

        assertEquals(
            1,
            onAllNodes(hasText("You already have an active subscription somewhere else."))
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aSupporterTheStoreHasNotCaughtUpWithIsStillOfferedAnUpgrade() = runComposeUiTest {
        showPaywall(
            store = everyTier,
            mode = PricingMode.CLOUD_ONLY,
            subscribedSku = supporter.sku,
        )

        assertShowing("Upgrade to \$29.99/year")
    }

    @Test
    fun aSupporterIsOfferedAnUpgradeRatherThanThanked() = runComposeUiTest {
        showPaywall(
            store = everyTier.copy(currentSku = supporter.sku),
            mode = PricingMode.CLOUD_ONLY,
        )

        assertShowing("Upgrade to \$29.99/year")
        assertNotShowing("Thanks for your support")
    }

    private companion object {
        const val CLOUD_TIER = "Tasks.org Cloud"
        const val SUPPORTER_TIER = "Tasks.org Supporter"
    }
}
