package org.tasks.compose.pricing

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.billing.StripeCatalog
import org.tasks.billing.StripePrice
import tasks.kmp.generated.resources.*

enum class PricingMode {
    CLOUD_ONLY,
    NYP_ONLY,
    BOTH,
}

private val CardShape = RoundedCornerShape(12.dp)

data class StoreBilling(
    val catalog: StripeCatalog = StripeCatalog(),
    val loadingPrices: Boolean = false,
    val busySku: String? = null,
    val currentSku: String? = null,
    val suggestedPrice: Int? = null,
    val onSelect: (StripePrice) -> Unit,
) {
    val busy: Boolean get() = busySku != null

    fun isBusy(price: StripePrice?): Boolean = price != null && busySku == price.sku

    fun unavailable(price: StripePrice?): Boolean = !loadingPrices && price == null
}

private val StripePrice?.trialNote: String?
    @Composable get() = this?.trialDays?.takeIf { it > 0 }?.let {
        stringResource(Res.string.pricing_free_trial, it)
    }

@Composable
private fun StripePrice?.ctaLabel(): String = if (this == null) {
    stringResource(Res.string.pricing_subscribe_google_play)
} else {
    stringResource(
        Res.string.pricing_subscribe_for,
        stringResource(
            if (isMonthly) Res.string.price_per_month_with_currency
            else Res.string.price_per_year_with_currency,
            formattedPrice,
        ),
    )
}

@Composable
private fun UnavailableNotice() {
    Text(
        text = stringResource(Res.string.error_billing_unavailable),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
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
    onBillingToggle: (isAnnual: Boolean) -> Unit = {},
    showSupporterBanner: Boolean = false,
    store: StoreBilling? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        SymbolIcon(
                            name = TasksIcons.ARROW_BACK,
                            contentDescription = stringResource(Res.string.back),
                        )
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
            val isWide = showBoth && maxWidth >= 840.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showSupporterBanner) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            text = stringResource(Res.string.upgrade_subscription_banner),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Text(
                    text = stringResource(Res.string.pricing_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .padding(bottom = 24.dp),
                )

                val cloudCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    CloudTierCard(
                        modifier = cardModifier,
                        isRecommended = showBoth,
                        onCtaClick = onCloudSubscribeClick,
                        onSignIn = onSignIn,
                        onBillingToggle = onBillingToggle,
                        store = store,
                    )
                }
                val nameYourPriceCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    NameYourPriceTierCard(
                        modifier = cardModifier,
                        onCtaClick = onNypSubscribeClick,
                        onRestorePurchases = onRestorePurchases,
                        store = store,
                    )
                }

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        cloudCard(Modifier.weight(1f).fillMaxHeight())
                        nameYourPriceCard(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(
                        modifier = Modifier.widthIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (mode != PricingMode.NYP_ONLY) {
                            cloudCard(Modifier.fillMaxWidth())
                        }
                        if (mode != PricingMode.CLOUD_ONLY) {
                            nameYourPriceCard(Modifier.fillMaxWidth())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CloudTierCard(
    modifier: Modifier = Modifier,
    isRecommended: Boolean = true,
    onCtaClick: () -> Unit,
    onSignIn: () -> Unit,
    onBillingToggle: (Boolean) -> Unit = {},
    store: StoreBilling? = null,
) {
    var isAnnual by rememberSaveable { mutableStateOf(true) }
    val price = if (isAnnual) store?.catalog?.cloudAnnual else store?.catalog?.cloudMonthly

    TierCard(
        modifier = modifier,
        title = stringResource(Res.string.tasks_org_account),
        price = price?.formattedPrice ?: stringResource(
            if (isAnnual) Res.string.pricing_cloud_annual_total
            else Res.string.pricing_cloud_monthly_price
        ),
        priceSuffix = stringResource(
            if (isAnnual) Res.string.price_per_year_with_currency
            else Res.string.price_per_month_with_currency,
            "",
        ).trim(),
        billingNote = if (isAnnual)
            stringResource(
                Res.string.pricing_cloud_annual_per_month,
                price?.formattedMonthlyPrice
                    ?: stringResource(Res.string.pricing_cloud_annual_price),
            )
        else
            stringResource(Res.string.pricing_cloud_monthly_note),
        trialNote = price.trialNote,
        description = stringResource(Res.string.pricing_cloud_description),
        features = listOf(
            stringResource(Res.string.pricing_cloud_feature_1),
            stringResource(Res.string.pricing_cloud_feature_2),
            stringResource(Res.string.pricing_cloud_feature_3),
            stringResource(Res.string.pricing_cloud_feature_4),
        ),
        cta = {
            if (store?.unavailable(price) == true) {
                UnavailableNotice()
            } else {
                PurchaseButton(
                    label = price.ctaLabel(),
                    busy = store?.loadingPrices == true || store?.isBusy(price) == true,
                    enabled = store == null ||
                        (!store.busy && (price == null || price.sku != store.currentSku)),
                    onClick = {
                        onCtaClick()
                        if (price != null) store?.onSelect(price)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        isRecommended = isRecommended,
        billingToggle = {
            BillingPeriodToggle(
                isAnnual = isAnnual,
                onToggle = {
                    isAnnual = it
                    onBillingToggle(it)
                },
            )
        },
        footerDescription = stringResource(Res.string.sign_in_subtitle),
        footerLabel = stringResource(Res.string.sign_in),
        onFooterClick = onSignIn,
    )
}

@Composable
private fun NameYourPriceTierCard(
    modifier: Modifier = Modifier,
    onCtaClick: () -> Unit,
    onRestorePurchases: () -> Unit,
    store: StoreBilling? = null,
) {
    val rungs = store?.catalog?.supporterRungs.orEmpty()
    val cheapestRung = rungs.firstOrNull()

    TierCard(
        modifier = modifier,
        title = stringResource(Res.string.supporter),
        price = stringResource(
            Res.string.pricing_from,
            cheapestRung?.formattedPrice ?: stringResource(Res.string.pricing_nyp_min_price),
        ),
        priceSuffix = stringResource(Res.string.price_per_year_with_currency, "").trim(),
        trialNote = cheapestRung.trialNote,
        description = stringResource(Res.string.pricing_nyp_description),
        features = listOf(
            stringResource(Res.string.pricing_nyp_feature_1),
            stringResource(Res.string.pricing_nyp_feature_2),
            stringResource(Res.string.pricing_nyp_feature_3),
        ),
        cta = {
            when {
                store != null && rungs.isNotEmpty() -> NameYourPricePicker(
                    store = store,
                    rungs = rungs,
                    onCtaClick = onCtaClick,
                )
                store?.unavailable(cheapestRung) == true -> UnavailableNotice()
                else -> PurchaseButton(
                    label = stringResource(Res.string.pricing_subscribe_google_play),
                    busy = store?.loadingPrices == true,
                    enabled = store?.busy != true,
                    onClick = onCtaClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        footerDescription = stringResource(Res.string.already_subscribed),
        footerLabel = stringResource(Res.string.restore_purchases),
        onFooterClick = onRestorePurchases,
    )
}

@Composable
private fun NameYourPricePicker(
    store: StoreBilling,
    rungs: List<StripePrice>,
    onCtaClick: () -> Unit,
) {
    var sliderPosition by rememberSaveable {
        mutableFloatStateOf(store.catalog.supporterRungFor(store.suggestedPrice).toFloat())
    }
    val card = store.catalog.supporterRungAt(sliderPosition) ?: return
    val monthly = store.catalog.supporterMonthlyFor(card)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..rungs.lastIndex.toFloat().coerceAtLeast(0f),
            steps = (rungs.size - 2).coerceAtLeast(0),
            enabled = !store.busy && rungs.size > 1,
            colors = SliderDefaults.colors(
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        val annual = stringResource(Res.string.price_per_year_with_currency, card.formattedPrice)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PurchaseButton(
                label = if (monthly == null) {
                    stringResource(Res.string.pricing_subscribe_for, annual)
                } else {
                    annual
                },
                busy = store.isBusy(card),
                enabled = !store.busy,
                onClick = {
                    onCtaClick()
                    store.onSelect(card)
                },
                modifier = Modifier.weight(1f),
            )
            if (monthly != null) {
                PurchaseButton(
                    label = stringResource(
                        Res.string.price_per_month_with_currency,
                        monthly.formattedPrice,
                    ),
                    busy = store.isBusy(monthly),
                    enabled = !store.busy,
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
    price: String,
    priceSuffix: String,
    description: String,
    features: List<String>,
    cta: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isRecommended: Boolean = false,
    billingNote: String? = null,
    trialNote: String? = null,
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
            billingNote = billingNote,
            trialNote = trialNote,
            billingToggle = billingToggle,
            description = description,
            features = features,
            cta = cta,
            footerDescription = footerDescription,
            footerLabel = footerLabel,
            onFooterClick = onFooterClick,
        )
    }
    Box(modifier = modifier.fillMaxHeight()) {
        if (isRecommended) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                shape = CardShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
                elevation = CardDefaults.outlinedCardElevation(
                    defaultElevation = 4.dp,
                ),
            ) {
                content()
            }
            // Badge
            Surface(
                modifier = Modifier.align(Alignment.TopCenter),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = stringResource(Res.string.pricing_recommended).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxSize().padding(top = 13.dp),
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun TierCardContent(
    title: String,
    price: String,
    priceSuffix: String,
    billingNote: String?,
    trialNote: String?,
    billingToggle: @Composable (() -> Unit)?,
    description: String,
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
            Text(
                text = price,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alignByBaseline(),
            )
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
        }

        if (trialNote != null) {
            Text(
                text = trialNote,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
        color = if (selected) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceContainerHigh,
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
