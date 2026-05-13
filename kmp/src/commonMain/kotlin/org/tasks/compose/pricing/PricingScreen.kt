package org.tasks.compose.pricing

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.*

enum class PricingMode {
    CLOUD_ONLY,
    NYP_ONLY,
    BOTH,
}

private val CardShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(
    mode: PricingMode = PricingMode.BOTH,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onRestorePurchases: () -> Unit,
    onCloudSubscribeClick: () -> Unit = {},
    onCloudSponsorClick: () -> Unit = {},
    onNypSubscribeClick: () -> Unit = {},
    onNypSponsorClick: () -> Unit = {},
    onBillingToggle: (isAnnual: Boolean) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                Text(
                    text = stringResource(Res.string.pricing_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .padding(bottom = 24.dp),
                )

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CloudTierCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            isRecommended = true,
                            onCtaClick = onCloudSubscribeClick,
                            onSponsorClick = onCloudSponsorClick,
                            onSignIn = onSignIn,
                            onBillingToggle = onBillingToggle,
                        )
                        NameYourPriceTierCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onCtaClick = onNypSubscribeClick,
                            onSponsorClick = onNypSponsorClick,
                            onRestorePurchases = onRestorePurchases,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.widthIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (mode != PricingMode.NYP_ONLY) {
                            CloudTierCard(
                                modifier = Modifier.fillMaxWidth(),
                                isRecommended = showBoth,
                                onCtaClick = onCloudSubscribeClick,
                                onSponsorClick = onCloudSponsorClick,
                                onSignIn = onSignIn,
                                onBillingToggle = onBillingToggle,
                            )
                        }
                        if (mode != PricingMode.CLOUD_ONLY) {
                            NameYourPriceTierCard(
                                modifier = Modifier.fillMaxWidth(),
                                onCtaClick = onNypSubscribeClick,
                                onSponsorClick = onNypSponsorClick,
                                onRestorePurchases = onRestorePurchases,
                            )
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
    onSponsorClick: () -> Unit,
    onSignIn: () -> Unit,
    onBillingToggle: (Boolean) -> Unit = {},
) {
    var isAnnual by remember { mutableStateOf(true) }

    TierCard(
        modifier = modifier,
        title = stringResource(Res.string.tasks_org_account),
        price = stringResource(
            if (isAnnual) Res.string.pricing_cloud_annual_price
            else Res.string.pricing_cloud_monthly_price
        ),
        priceSuffix = stringResource(Res.string.price_per_month_with_currency, "").trim(),
        billingNote = if (isAnnual)
            stringResource(
                Res.string.pricing_cloud_annual_note,
                stringResource(Res.string.price_per_year_with_currency, stringResource(Res.string.pricing_cloud_annual_total)),
            )
        else
            stringResource(Res.string.pricing_cloud_monthly_note),
        description = stringResource(Res.string.pricing_cloud_description),
        features = listOf(
            stringResource(Res.string.pricing_cloud_feature_1),
            stringResource(Res.string.pricing_cloud_feature_2),
            stringResource(Res.string.pricing_cloud_feature_3),
            stringResource(Res.string.pricing_cloud_feature_4),
        ),
        ctaLabel = stringResource(Res.string.pricing_subscribe_google_play),
        onCtaClick = onCtaClick,
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
        secondaryLabel = stringResource(
            Res.string.pricing_sponsor,
            if (isAnnual)
                stringResource(Res.string.price_per_year_with_currency, stringResource(Res.string.pricing_sponsor_annual))
            else
                stringResource(Res.string.price_per_month_with_currency, stringResource(Res.string.pricing_sponsor_monthly)),
        ),
        onSecondaryClick = onSponsorClick,
        footerDescription = stringResource(Res.string.sign_in_subtitle),
        footerLabel = stringResource(Res.string.sign_in),
        onFooterClick = onSignIn,
    )
}

@Composable
private fun NameYourPriceTierCard(
    modifier: Modifier = Modifier,
    onCtaClick: () -> Unit,
    onSponsorClick: () -> Unit,
    onRestorePurchases: () -> Unit,
) {
    TierCard(
        modifier = modifier,
        title = stringResource(Res.string.pricing_nyp_title),
        price = stringResource(Res.string.pricing_from, stringResource(Res.string.pricing_nyp_min_price)),
        priceSuffix = stringResource(Res.string.price_per_year_with_currency, "").trim(),
        description = stringResource(Res.string.pricing_nyp_description),
        features = listOf(
            stringResource(Res.string.pricing_nyp_feature_1),
            stringResource(Res.string.pricing_nyp_feature_2),
            stringResource(Res.string.pricing_nyp_feature_3),
        ),
        ctaLabel = stringResource(Res.string.pricing_subscribe_google_play),
        onCtaClick = onCtaClick,
        secondaryLabel = stringResource(
            Res.string.pricing_sponsor,
            stringResource(Res.string.price_per_year_with_currency, stringResource(Res.string.pricing_sponsor_nyp)),
        ),
        onSecondaryClick = onSponsorClick,
        footerDescription = stringResource(Res.string.already_subscribed),
        footerLabel = stringResource(Res.string.restore_purchases),
        onFooterClick = onRestorePurchases,
    )
}

@Composable
private fun TierCard(
    title: String,
    price: String,
    priceSuffix: String,
    description: String,
    features: List<String>,
    ctaLabel: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRecommended: Boolean = false,
    billingNote: String? = null,
    billingToggle: @Composable (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    footerDescription: String? = null,
    footerLabel: String? = null,
    onFooterClick: (() -> Unit)? = null,
) {
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
                TierCardContent(
                    title = title,
                    price = price,
                    priceSuffix = priceSuffix,
                    billingNote = billingNote,
                    billingToggle = billingToggle,
                    description = description,
                    features = features,
                    ctaLabel = ctaLabel,
                    onCtaClick = onCtaClick,
                    secondaryLabel = secondaryLabel,
                    onSecondaryClick = onSecondaryClick,
                    footerDescription = footerDescription,
                    footerLabel = footerLabel,
                    onFooterClick = onFooterClick,
                )
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
                TierCardContent(
                    title = title,
                    price = price,
                    priceSuffix = priceSuffix,
                    billingNote = billingNote,
                    billingToggle = billingToggle,
                    description = description,
                    features = features,
                    ctaLabel = ctaLabel,
                    onCtaClick = onCtaClick,
                    secondaryLabel = secondaryLabel,
                    onSecondaryClick = onSecondaryClick,
                    footerDescription = footerDescription,
                    footerLabel = footerLabel,
                    onFooterClick = onFooterClick,
                )
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
    billingToggle: @Composable (() -> Unit)?,
    description: String,
    features: List<String>,
    ctaLabel: String,
    onCtaClick: () -> Unit,
    secondaryLabel: String?,
    onSecondaryClick: (() -> Unit)?,
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

        Button(
            onClick = onCtaClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(ctaLabel)
        }

        if (secondaryLabel != null && onSecondaryClick != null) {
            TextButton(
                onClick = onSecondaryClick,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

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
            Icon(
                imageVector = Icons.Default.Check,
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
