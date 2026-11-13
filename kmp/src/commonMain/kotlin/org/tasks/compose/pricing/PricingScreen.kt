package org.tasks.compose.pricing

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.PaywallAnalytics
import org.tasks.billing.BillingCatalog
import org.tasks.billing.BillingOption
import org.tasks.billing.SubscriptionSkus
import org.tasks.billing.UnlockSource
import tasks.kmp.generated.resources.*

enum class PricingMode {
    CLOUD_ONLY,
    NYP_ONLY,
    BOTH,
}

const val SOURCE_MANAGE_SUBSCRIPTION = "manage_subscription"

private val UPGRADE_SOURCES = setOf(AnalyticsEvents.SOURCE_SETTINGS, SOURCE_MANAGE_SUBSCRIPTION)

fun leadsWithSupporter(source: String): Boolean = source !in UPGRADE_SOURCES

internal fun isCloudSku(sku: String?, catalog: BillingCatalog?): Boolean =
    sku != null && (SubscriptionSkus.isTasksSubscription(sku) || catalog?.price(sku)?.entitles == true)

internal fun isSupporterSku(sku: String?, catalog: BillingCatalog?): Boolean =
    sku != null && !isCloudSku(sku, catalog) &&
            (catalog?.price(sku) != null || SubscriptionSkus.supporterPrice(sku) != null)

enum class PlanMove {
    SUBSCRIBE,
    UPGRADE,
    CHANGE,
}

enum class SupporterFeatures {
    APP_STORE,
    ANDROID_CARD,
    DESKTOP,
}

private val CardShape = RoundedCornerShape(12.dp)

private const val BADGE_FADE_MILLIS = 300

private const val PRICE_UNKNOWN = "\u2014"

data class StoreBilling(
    val catalog: BillingCatalog = BillingCatalog(),
    val loadingPrices: Boolean = false,
    val busySku: String? = null,
    val currentSku: String? = null,
    val sellsCurrentSku: Boolean = false,
    val loadingPlan: Boolean = false,
    val knownSku: String? = null,
    val suggestedPrice: Int? = null,
    val knownCustomer: Boolean? = false,
    val coveredElsewhere: Boolean = false,
    val coveredBy: UnlockSource? = null,
    val onSelect: (BillingOption) -> Unit,
) {
    val busy: Boolean get() = busySku != null

    fun isBusy(price: BillingOption?): Boolean = price != null && busySku == price.sku

    fun unavailable(price: BillingOption?): Boolean = !loadingPrices && price == null

    private val ownedSku: String?
        get() = currentSku ?: knownSku.takeIf { loadingPlan }

    fun selectable(price: BillingOption?): Boolean =
        !busy && (price == null || price.sku != ownedSku || sellsCurrentSku)

    fun move(price: BillingOption): PlanMove {
        val current = (currentSku ?: knownSku)?.takeIf { it != price.sku } ?: return PlanMove.SUBSCRIBE
        val currentIsCloud = isCloudSku(current, catalog)
        return when {
            price.entitles != currentIsCloud ->
                if (price.entitles) PlanMove.UPGRADE else PlanMove.CHANGE
            price.entitles -> PlanMove.CHANGE
            else -> {
                val tier = catalog.price(current)?.rungDollars
                    ?: SubscriptionSkus.subscriptionPrice(current)
                    ?: return PlanMove.SUBSCRIBE
                if (price.rungDollars > tier) PlanMove.UPGRADE else PlanMove.CHANGE
            }
        }
    }

    fun owns(price: BillingOption?): Boolean =
        price != null && price.sku == ownedSku && !sellsCurrentSku

    val supporterPrice: BillingOption?
        get() = ownedSku?.takeIf { isSupporterSku(it, catalog) }?.let { catalog.price(it) }

    val awaitingPlan: Boolean
        get() = loadingPlan && ownedSku == null

    fun unavailable(cloud: Boolean, supporter: Boolean): Boolean =
        !loadingPrices && (
                cloud && catalog.cloud.isEmpty() ||
                        supporter && catalog.supporterRungs.isEmpty()
                )

    fun warnCoveredElsewhere(cloud: Boolean, supporter: Boolean): Boolean =
        coveredElsewhere && (
                cloud && catalog.cloud.isNotEmpty() && (currentSku == null || !currentlyEntitled) ||
                        supporter && catalog.supporterRungs.isNotEmpty() && currentSku == null
                )

    private val currentlyEntitled: Boolean
        get() = SubscriptionSkus.isTasksSubscription(currentSku) ||
                catalog.price(currentSku)?.entitles == true
}

@Composable
private fun BillingOption?.trialFeature(store: StoreBilling?): String? =
    if (store?.knownCustomer == false && (this?.trialDays ?: 0) > 0) {
        stringResource(Res.string.pricing_feature_trial)
    } else {
        null
    }

@Composable
private fun BillingOption?.ctaLabel(store: StoreBilling?): String = if (this == null) {
    stringResource(Res.string.pricing_subscribe_google_play)
} else {
    actionLabel(store)
}

@Composable
private fun BillingOption.actionLabel(store: StoreBilling?): String = stringResource(
    when (store?.move(this)) {
        PlanMove.UPGRADE -> Res.string.pricing_upgrade_to
        PlanMove.CHANGE -> Res.string.pricing_change_to
        else -> Res.string.pricing_subscribe_for
    },
    intervalPrice(formattedPrice, isMonthly),
)

@Composable
private fun BillingOption.ownedOr(store: StoreBilling, label: String): String =
    if (store.owns(this)) stringResource(Res.string.pricing_subscribed) else label

@Composable
private fun RestoreFooter(onRestorePurchases: () -> Unit) {
    TextButton(
        onClick = onRestorePurchases,
        modifier = Modifier.padding(top = 16.dp),
    ) {
        Text(stringResource(Res.string.restore_purchases))
    }
}

@Composable
private fun DeveloperNote() {
    Card(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
            Column(modifier = Modifier.padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)) {
                SymbolIcon(
                    name = TasksIcons.FORMAT_QUOTE,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    filled = true,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = stringResource(Res.string.upgrade_blurb_1),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.upgrade_blurb_2),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PricingNotice(text: String) {
    Card(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(
                name = TasksIcons.WARNING,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(
    mode: PricingMode = PricingMode.BOTH,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onRestorePurchases: () -> Unit,
    onCloudSubscribeClick: () -> Unit = {},
    onNypSubscribeClick: () -> Unit = {},
    showSignIn: Boolean = true,
    supporterFeatures: SupporterFeatures = SupporterFeatures.APP_STORE,
    supporterFirst: Boolean = false,
    subscribedSku: String? = null,
    store: StoreBilling? = null,
    analytics: PaywallAnalytics? = null,
) {
    @Suppress("NAME_SHADOWING")
    val store = store?.copy(knownSku = subscribedSku)
    val signIn = {
        analytics?.signInClicked()
        onSignIn()
    }
    val currentSku = store?.currentSku ?: subscribedSku
    val cloudIsCurrent = isCloudSku(currentSku, store?.catalog)
    val supporterIsCurrent = isSupporterSku(currentSku, store?.catalog)
    val cloudAnnual = rememberSaveable { mutableStateOf(true) }
    val nypSelection = rememberSaveable(saver = NameYourPriceSelection.Saver) {
        NameYourPriceSelection()
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        SymbolIcon(
                            name = TasksIcons.ARROW_BACK,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
                actions = {
                    if (showSignIn && mode != PricingMode.CLOUD_ONLY) {
                        TextButton(onClick = signIn) {
                            Text(stringResource(Res.string.sign_in))
                        }
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val showBoth = mode == PricingMode.BOTH
            val cloudOnly = mode == PricingMode.CLOUD_ONLY
            val showCloud = mode != PricingMode.NYP_ONLY
            val showNyp = mode != PricingMode.CLOUD_ONLY
            val isWide = showBoth && maxWidth >= 840.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DeveloperNote()

                if (store?.unavailable(cloud = showCloud, supporter = showNyp) == true) {
                    PricingNotice(stringResource(Res.string.error_billing_unavailable))
                }
                if (store?.warnCoveredElsewhere(cloud = showCloud, supporter = showNyp) == true) {
                    PricingNotice(coveredElsewhereMessage(store.coveredBy))
                }

                val cloudCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    CloudTierCard(
                        modifier = cardModifier,
                        isCurrent = cloudIsCurrent,
                        isRecommended = showBoth &&
                                !supporterFirst &&
                                supporterFeatures != SupporterFeatures.ANDROID_CARD &&
                                !supporterIsCurrent,
                        onCtaClick = {
                            analytics?.subscribeClicked(cloud = true)
                            onCloudSubscribeClick()
                        },
                        onSignIn = signIn.takeIf { showSignIn && cloudOnly },
                        onBillingToggle = { analytics?.billingToggled(it) },
                        store = store,
                        annual = cloudAnnual,
                    )
                }
                val nameYourPriceCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    NameYourPriceTierCard(
                        modifier = cardModifier,
                        onCtaClick = {
                            analytics?.subscribeClicked(cloud = false)
                            onNypSubscribeClick()
                        },
                        onSelect = { selection, price ->
                            analytics?.nameYourPriceSelected(selection, price)
                        },
                        isCurrent = supporterIsCurrent,
                        features = supporterFeatures,
                        store = store,
                        selection = nypSelection,
                    )
                }

                val cards = listOfNotNull(
                    cloudCard.takeIf { showCloud },
                    nameYourPriceCard.takeIf { showNyp },
                ).let {
                    if (supporterFirst || supporterFeatures == SupporterFeatures.ANDROID_CARD) {
                        it.reversed()
                    } else {
                        it
                    }
                }

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        cards.forEach { card -> card(Modifier.weight(1f).fillMaxHeight()) }
                    }
                } else {
                    Column(
                        modifier = Modifier.widthIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        cards.forEach { card -> card(Modifier.fillMaxWidth()) }
                    }
                }

                RestoreFooter(onRestorePurchases)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CloudTierCard(
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    isRecommended: Boolean = true,
    onCtaClick: () -> Unit,
    onSignIn: (() -> Unit)?,
    onBillingToggle: (Boolean) -> Unit = {},
    store: StoreBilling? = null,
    annual: MutableState<Boolean> = remember { mutableStateOf(true) },
) {
    var isAnnual by annual
    val price = if (isAnnual) store?.catalog?.cloudAnnual else store?.catalog?.cloudMonthly

    TierCard(
        modifier = modifier,
        badge = when {
            isCurrent -> stringResource(Res.string.pricing_current_subscription)
            isRecommended -> stringResource(Res.string.pricing_recommended)
            else -> null
        },
        title = stringResource(Res.string.tasks_org_account),
        price = if (store == null) {
            stringResource(
                if (isAnnual) Res.string.pricing_cloud_annual_total
                else Res.string.pricing_cloud_monthly_price
            )
        } else {
            price?.formattedPrice
        },
        loading = store?.loadingPrices == true,
        priceSuffix = stringResource(
            if (isAnnual) Res.string.price_per_year_with_currency
            else Res.string.price_per_month_with_currency,
            "",
        ).trim(),
        hasBillingNote = true,
        billingNote = if (isAnnual) {
            val perMonth = if (store == null) {
                stringResource(Res.string.pricing_cloud_annual_price)
            } else {
                price?.formattedMonthlyPrice
            }
            perMonth?.let { stringResource(Res.string.pricing_cloud_annual_per_month, it) }
        } else {
            stringResource(Res.string.pricing_cloud_monthly_note)
        },
        description = stringResource(Res.string.pricing_cloud_description),
        features = listOf(
            stringResource(Res.string.pricing_cloud_feature_1_v2),
            stringResource(Res.string.pricing_cloud_feature_2),
            stringResource(Res.string.pricing_cloud_feature_3),
        ) + listOfNotNull(price.trialFeature(store)),
        cta = {
            if (store?.unavailable(price) != true) {
                PurchaseButton(
                    label = price.ctaLabel(store).let { label ->
                        if (store != null && price != null) price.ownedOr(store, label) else label
                    },
                    busy = store?.loadingPrices == true || store?.isBusy(price) == true,
                    enabled = store == null || store.selectable(price),
                    onClick = {
                        onCtaClick()
                        if (price != null) store?.onSelect(price)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        billingToggle = {
            BillingPeriodToggle(
                isAnnual = isAnnual,
                onToggle = {
                    if (it != isAnnual) {
                        isAnnual = it
                        onBillingToggle(it)
                    }
                },
            )
        },
        footerDescription = stringResource(Res.string.sign_in_subtitle)
            .takeIf { onSignIn != null },
        footerLabel = stringResource(Res.string.sign_in).takeIf { onSignIn != null },
        onFooterClick = onSignIn,
    )
}

@Composable
private fun NameYourPriceTierCard(
    modifier: Modifier = Modifier,
    onCtaClick: () -> Unit,
    onSelect: (selection: String, price: Int?) -> Unit = { _, _ -> },
    isCurrent: Boolean = false,
    features: SupporterFeatures = SupporterFeatures.APP_STORE,
    store: StoreBilling? = null,
    selection: NameYourPriceSelection = remember { NameYourPriceSelection() },
) {
    val rungs = store?.catalog?.supporterRungs.orEmpty()
    val cheapestRung = rungs.firstOrNull()
    val chosenSku = selection.sku
    val shown = store?.let { billing ->
        chosenSku?.let { billing.catalog.price(it) }
            ?: billing.supporterPrice
    }

    TierCard(
        modifier = modifier,
        badge = stringResource(Res.string.pricing_current_subscription).takeIf { isCurrent },
        title = stringResource(Res.string.supporter),
        price = when {
            store == null -> stringResource(
                Res.string.pricing_from,
                stringResource(Res.string.pricing_nyp_min_price),
            )
            shown != null -> shown.formattedPrice
            else -> cheapestRung?.formattedPrice?.let { stringResource(Res.string.pricing_from, it) }
        },
        loading = store != null && (store.loadingPrices || chosenSku == null && store.awaitingPlan),
        priceSuffix = stringResource(
            if (shown?.isMonthly == true) {
                Res.string.price_per_month_with_currency
            } else {
                Res.string.price_per_year_with_currency
            },
            "",
        ).trim(),
        description = stringResource(Res.string.pricing_nyp_description)
            .takeIf { features != SupporterFeatures.ANDROID_CARD },
        features = when (features) {
            SupporterFeatures.APP_STORE -> listOf(
                stringResource(Res.string.pricing_nyp_feature_1),
                stringResource(Res.string.pricing_nyp_feature_2),
                stringResource(Res.string.pricing_nyp_feature_3),
            )
            SupporterFeatures.ANDROID_CARD -> listOf(
                stringResource(Res.string.pricing_nyp_feature_desktop_and_ios),
                stringResource(Res.string.pricing_nyp_feature_no_donation_reminders),
                stringResource(Res.string.pricing_nyp_feature_3),
            )
            SupporterFeatures.DESKTOP -> listOf(
                stringResource(Res.string.pricing_nyp_feature_all_platforms),
                stringResource(Res.string.pricing_nyp_feature_1),
                stringResource(Res.string.pricing_nyp_feature_third_party_sync),
                stringResource(Res.string.pricing_nyp_feature_3),
            )
        } + listOfNotNull(
            cheapestRung.trialFeature(store).takeIf { features != SupporterFeatures.ANDROID_CARD }
        ),
        cta = {
            when {
                store != null && rungs.isNotEmpty() -> NameYourPricePicker(
                    store = store,
                    rungs = rungs,
                    onCtaClick = onCtaClick,
                    onSelect = onSelect,
                    selection = selection,
                )
                store?.unavailable(cheapestRung) == true -> Unit
                else -> PurchaseButton(
                    label = stringResource(Res.string.pricing_subscribe_google_play),
                    busy = store?.loadingPrices == true,
                    enabled = store?.busy != true,
                    onClick = onCtaClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Stable
internal class NameYourPriceSelection(sku: String? = null, custom: Boolean? = null) {
    var sku by mutableStateOf(sku)
    var custom by mutableStateOf(custom)

    companion object {
        val Saver: Saver<NameYourPriceSelection, Any> = listSaver(
            save = { listOf(it.sku, it.custom) },
            restore = { NameYourPriceSelection(it[0] as String?, it[1] as Boolean?) },
        )
    }
}

internal val PRESET_TIERS = listOf(3, 5, 10, 20)

internal fun presetRungs(rungs: List<BillingOption>): List<Int> =
    PRESET_TIERS.mapNotNull { tier ->
        rungs.indexOfFirst { it.rungDollars == tier }.takeIf { it >= 0 }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NameYourPricePicker(
    store: StoreBilling,
    rungs: List<BillingOption>,
    onCtaClick: () -> Unit,
    onSelect: (selection: String, price: Int?) -> Unit,
    selection: NameYourPriceSelection,
) {
    var sliderPosition by remember(rungs) {
        mutableFloatStateOf(
            (rungs.indexOfFirst { it.sku == selection.sku }.takeIf { it >= 0 }
                ?: store.catalog.supporterRungFor(store.suggestedPrice)).toFloat()
        )
    }
    val presets = remember(rungs) { presetRungs(rungs) }
    var dragStart by remember(rungs) { mutableStateOf<Int?>(null) }
    val custom = selection.custom ?: (sliderPosition.roundToInt() !in presets)
    val onChosen: (BillingOption) -> Unit = { selection.sku = it.sku }
    val card = store.catalog.supporterRungAt(sliderPosition) ?: return
    val monthly = store.catalog.supporterMonthlyFor(card)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (custom) {
            Slider(
                value = sliderPosition,
                onValueChange = {
                    if (dragStart == null) dragStart = sliderPosition.roundToInt()
                    sliderPosition = it
                    selection.custom = true
                    store.catalog.supporterRungAt(it)?.let(onChosen)
                },
                onValueChangeFinished = {
                    val start = dragStart
                    dragStart = null
                    if (start != null && start != sliderPosition.roundToInt()) {
                        onSelect(
                            AnalyticsEvents.SELECTION_SLIDER,
                            store.catalog.supporterRungAt(sliderPosition)?.rungDollars,
                        )
                    }
                },
                valueRange = 0f..rungs.lastIndex.toFloat().coerceAtLeast(0f),
                steps = (rungs.size - 2).coerceAtLeast(0),
                enabled = !store.busy && rungs.size > 1,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                presets.forEach { index ->
                    FilterChip(
                        selected = sliderPosition.roundToInt() == index,
                        onClick = {
                            val changed = sliderPosition.roundToInt() != index
                            sliderPosition = index.toFloat()
                            onChosen(rungs[index])
                            if (changed) {
                                onSelect(AnalyticsEvents.SELECTION_PRESET, rungs[index].rungDollars)
                            }
                        },
                        label = { Text(rungs[index].formattedPrice) },
                        enabled = !store.busy,
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = {
                        selection.custom = true
                        onSelect(AnalyticsEvents.SELECTION_CUSTOM, null)
                    },
                    label = { Text(stringResource(Res.string.pricing_custom)) },
                    enabled = !store.busy,
                )
            }
        }
        val annual = stringResource(Res.string.price_per_year_with_currency, card.formattedPrice)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PurchaseButton(
                label = card.ownedOr(
                    store,
                    if (monthly == null) card.actionLabel(store) else annual,
                ),
                busy = store.isBusy(card),
                enabled = store.selectable(card),
                onClick = {
                    onCtaClick()
                    store.onSelect(card)
                },
                modifier = Modifier.weight(1f),
            )
            if (monthly != null) {
                PurchaseButton(
                    label = monthly.ownedOr(
                        store,
                        stringResource(
                            Res.string.price_per_month_with_currency,
                            monthly.formattedPrice,
                        ),
                    ),
                    busy = store.isBusy(monthly),
                    enabled = store.selectable(monthly),
                    onClick = {
                        onCtaClick()
                        store.onSelect(monthly)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PurchaseButton(
    label: String,
    busy: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(label)
        }
    }
}

@Composable
private fun TierCard(
    title: String,
    price: String?,
    priceSuffix: String,
    loading: Boolean = false,
    description: String?,
    features: List<String>,
    cta: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    billingNote: String? = null,
    hasBillingNote: Boolean = false,
    billingToggle: @Composable (() -> Unit)? = null,
    footerDescription: String? = null,
    footerLabel: String? = null,
    onFooterClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        TierCardContent(
            title = title,
            price = price,
            priceSuffix = priceSuffix,
            loading = loading,
            billingNote = billingNote,
            hasBillingNote = hasBillingNote,
            billingToggle = billingToggle,
            description = description,
            features = features,
            cta = cta,
            footerDescription = footerDescription,
            footerLabel = footerLabel,
            onFooterClick = onFooterClick,
        )
    }
    val highlighted = badge != null
    val borderColor by animateColorAsState(
        if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(BADGE_FADE_MILLIS),
    )
    val borderWidth by animateDpAsState(
        if (highlighted) 2.dp else 1.dp,
        animationSpec = tween(BADGE_FADE_MILLIS),
    )
    val elevation by animateDpAsState(
        if (highlighted) 4.dp else 0.dp,
        animationSpec = tween(BADGE_FADE_MILLIS),
    )
    Box(modifier = modifier.fillMaxHeight()) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp),
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
            border = BorderStroke(borderWidth, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        ) {
            content()
        }
        Crossfade(
            targetState = badge,
            animationSpec = tween(BADGE_FADE_MILLIS),
            modifier = Modifier.align(Alignment.TopCenter),
            label = "badge",
        ) { text ->
            if (text != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = text.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TierCardContent(
    title: String,
    price: String?,
    priceSuffix: String,
    loading: Boolean,
    billingNote: String?,
    hasBillingNote: Boolean,
    billingToggle: @Composable (() -> Unit)?,
    description: String?,
    features: List<String>,
    cta: @Composable () -> Unit,
    footerDescription: String?,
    footerLabel: String?,
    onFooterClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (billingToggle != null) {
            billingToggle()
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            horizontalArrangement = Arrangement.Center,
        ) {
            when {
                price != null -> Text(
                    text = price,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignByBaseline(),
                )
                loading -> PriceShimmer(
                    sample = PRICE_SAMPLE,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignByBaseline(),
                )
                else -> Text(
                    text = PRICE_UNKNOWN,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = priceSuffix,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }

        if (billingNote != null) {
            Text(
                text = billingNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else if (loading && hasBillingNote) {
            PriceShimmer(
                sample = PRICE_NOTE_SAMPLE,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (description != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            features.forEachIndexed { index, feature ->
                FeatureRow(
                    text = feature,
                    showDivider = index < features.lastIndex,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f).height(24.dp))

        cta()

        if (footerLabel != null && onFooterClick != null) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            if (footerDescription != null) {
                Text(
                    text = footerDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            Button(
                onClick = onFooterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(footerLabel)
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureRow(
    text: String,
    showDivider: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SymbolIcon(
                name = TasksIcons.CHECK,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
private fun BillingPeriodToggle(
    isAnnual: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
        ) {
            TogglePill(
                text = stringResource(Res.string.annual),
                selected = isAnnual,
                onClick = { onToggle(true) },
            )
            TogglePill(
                text = stringResource(Res.string.monthly),
                selected = !isAnnual,
                onClick = { onToggle(false) },
            )
        }
    }
}

@Composable
private fun TogglePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerLowest
        else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
