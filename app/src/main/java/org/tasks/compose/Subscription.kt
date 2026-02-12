package org.tasks.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.billing.Sku
import org.tasks.compose.Constants.HALF_KEYLINE
import org.tasks.compose.Constants.KEYLINE_FIRST
import org.tasks.compose.PurchaseText.SubscriptionScreen
import org.tasks.extensions.Context.openUri
import org.tasks.themes.TasksTheme

object PurchaseText {
    private const val POPPER = "\uD83C\uDF89"

    enum class IconStyle { TINT, ORIGINAL, GRAYSCALE }

    data class CarouselItem(
        val title: Int,
        val icon: Int,
        val description: Int,
        val iconStyle: IconStyle = IconStyle.TINT,
    )

    private val nameYourPriceFeatureList = listOf(
        CarouselItem(
            R.string.upgrade_more_customization,
            R.drawable.ic_outline_palette_24px,
            R.string.upgrade_more_customization_description
        ),
        CarouselItem(
            R.string.open_source,
            R.drawable.ic_outline_favorite_border_24px,
            R.string.upgrade_open_source_description
        ),
        CarouselItem(
            R.string.tasks_org_account,
            R.drawable.ic_round_icon,
            R.string.account_not_included,
            iconStyle = IconStyle.GRAYSCALE
        ),
        CarouselItem(
            R.string.davx5,
            R.drawable.ic_davx5_icon_green_bg,
            R.string.davx5_selection_description,
            iconStyle = IconStyle.ORIGINAL
        ),
        CarouselItem(
            R.string.caldav,
            R.drawable.ic_webdav_logo,
            R.string.caldav_selection_description
        ),
        CarouselItem(
            R.string.etesync,
            R.drawable.ic_etesync,
            R.string.etesync_selection_description,
            iconStyle = IconStyle.ORIGINAL
        ),
        CarouselItem(
            R.string.decsync,
            R.drawable.ic_decsync,
            R.string.decsync_selection_description,
            iconStyle = IconStyle.ORIGINAL
        ),
    )

    private val tasksOrgFeatureList = listOf(
        CarouselItem(
            R.string.tasks_org_account,
            R.drawable.ic_round_icon,
            R.string.upgrade_tasks_org_account_description,
            iconStyle = IconStyle.ORIGINAL
        ),
        CarouselItem(
            R.string.email_to_task,
            R.drawable.ic_outline_email_24px,
            R.string.upgrade_email_to_task_description
        ),
        CarouselItem(
            R.string.upgrade_more_customization,
            R.drawable.ic_outline_palette_24px,
            R.string.upgrade_more_customization_description
        ),
        CarouselItem(
            R.string.upgrade_desktop_access,
            R.drawable.ic_outline_computer_24px,
            R.string.upgrade_desktop_access_description
        ),
        CarouselItem(
            R.string.open_source,
            R.drawable.ic_outline_favorite_border_24px,
            R.string.upgrade_open_source_description
        ),
        CarouselItem(
            R.string.advanced_tools,
            R.drawable.ic_outline_build_24px,
            R.string.advanced_tools_description
        ),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SubscriptionScreen(
        nameYourPrice: Boolean,
        sliderPosition: Float,
        feature: Int = 0,
        github: Boolean = false,
        showMoreOptions: Boolean = true,
        existingSubscriber: Boolean = false,
        onSignIn: () -> Unit = {},
        snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
        setPrice: (Float) -> Unit,
        setNameYourPrice: (Boolean) -> Unit,
        subscribe: (Int, Boolean) -> Unit,
        skus: List<Sku>,
        onBack: () -> Unit,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(
                                if (nameYourPrice) R.string.name_your_price
                                else R.string.upgrade_to_pro
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    actions = {
                        if (existingSubscriber) {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = null,
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sign_in)) },
                                    onClick = {
                                        expanded = false
                                        onSignIn()
                                    },
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(color = colorResource(R.color.content_background)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (existingSubscriber) {
                    ElevatedCard(
                        modifier = Modifier
                            .widthIn(max = 480.dp)
                            .fillMaxWidth()
                            .padding(KEYLINE_FIRST, KEYLINE_FIRST, KEYLINE_FIRST, 0.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.upgrade_subscription_banner),
                            modifier = Modifier.padding(KEYLINE_FIRST),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                OutlinedCard(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .padding(KEYLINE_FIRST, KEYLINE_FIRST, KEYLINE_FIRST, 0.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    GreetingText(R.string.upgrade_blurb_1)
                    GreetingText(R.string.upgrade_blurb_2)
                    Spacer(Modifier.height(KEYLINE_FIRST))
                }
                Spacer(Modifier.height(KEYLINE_FIRST))
                val scrollState = rememberScrollState()
                val items = remember(nameYourPrice, feature) {
                    if (nameYourPrice) {
                        val reordered = nameYourPriceFeatureList.toMutableList()
                        if (feature != 0) {
                            val index = reordered.indexOfFirst { it.title == feature }
                            if (index > 0) {
                                reordered.add(0, reordered.removeAt(index))
                            }
                        }
                        reordered
                    } else {
                        tasksOrgFeatureList
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .height(IntrinsicSize.Max)
                        .padding(horizontal = KEYLINE_FIRST),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                ) {
                    items.forEach { item ->
                        PagerItem(feature = item)
                    }
                }
                if (github) {
                    SponsorButton()
                } else {
                    GooglePlayButtons(
                        nameYourPrice = nameYourPrice,
                        sliderPosition = sliderPosition,
                        scrollState = scrollState,
                        showMoreOptions = showMoreOptions,
                        existingSubscriber = existingSubscriber,
                        onSignIn = onSignIn,
                        setNameYourPrice = setNameYourPrice,
                        setPrice = setPrice,
                        subscribe = subscribe,
                        skus = skus,
                    )
                }
            }
        }
    }

    @Composable
    fun SponsorButton() {
        val context = LocalContext.current
        OutlinedButton(
            onClick = { context.openUri(R.string.url_sponsor) },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            modifier = Modifier.padding(KEYLINE_FIRST, 0.dp, KEYLINE_FIRST, KEYLINE_FIRST)
        ) {
            Row {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_favorite_border_24px),
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.github_sponsor),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    @Composable
    fun GreetingText(resId: Int) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KEYLINE_FIRST, KEYLINE_FIRST, KEYLINE_FIRST, 0.dp),
            text = stringResource(resId),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }

    @Composable
    fun GooglePlayButtons(
        nameYourPrice: Boolean,
        sliderPosition: Float,
        scrollState: ScrollState,
        showMoreOptions: Boolean = true,
        existingSubscriber: Boolean = false,
        onSignIn: () -> Unit = {},
        setNameYourPrice: (Boolean) -> Unit,
        setPrice: (Float) -> Unit,
        subscribe: (Int, Boolean) -> Unit,
        skus: List<Sku>,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalDivider(modifier = Modifier.padding(vertical = KEYLINE_FIRST))
            val loading = skus.isEmpty()
            Box(
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (loading) 0f else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.pro_free_trial),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth(.75f)
                            .padding(bottom = KEYLINE_FIRST),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    if (nameYourPrice) {
                        NameYourPrice(
                            sliderPosition = sliderPosition,
                            setPrice = setPrice,
                            subscribe = subscribe,
                            skus = skus,
                        )
                    } else {
                        TasksAccount(
                            skus = skus,
                            subscribe = subscribe
                        )
                    }
                    if (showMoreOptions) {
                        Spacer(Modifier.height(KEYLINE_FIRST))
                        val scope = rememberCoroutineScope()
                        OutlinedButton(
                            onClick = {
                                setNameYourPrice(!nameYourPrice)
                                scope.launch {
                                    scrollState.animateScrollTo(0)
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = stringResource(
                                    if (nameYourPrice) R.string.more_options
                                    else R.string.name_your_price
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                if (loading) {
                    Spacer(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = {}
                            )
                    )
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            if (!showMoreOptions && !existingSubscriber) {
                Spacer(Modifier.height(KEYLINE_FIRST))
                OutlinedButton(
                    onClick = onSignIn,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(
                        text = stringResource(R.string.already_subscribed),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    @Composable
    fun PagerItem(feature: CarouselItem) {
        Column(
            modifier = Modifier
                .width(150.dp)
                .padding(HALF_KEYLINE),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(feature.icon),
                contentDescription = null,
                modifier = Modifier.requiredSize(72.dp),
                alignment = Alignment.Center,
                colorFilter = when (feature.iconStyle) {
                    IconStyle.GRAYSCALE -> ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToSaturation(0f) }
                    )
                    IconStyle.TINT -> ColorFilter.tint(colorResource(R.color.icon_tint_with_alpha))
                    IconStyle.ORIGINAL -> null
                }
            )
            Text(
                text = stringResource(feature.title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 4.dp),
                color = MaterialTheme.colorScheme.onBackground,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.25.sp
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(feature.description),
                modifier = Modifier.fillMaxWidth(),
                color = if (feature.iconStyle == IconStyle.GRAYSCALE)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onBackground,
                style = TextStyle(
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp
                ),
                textAlign = TextAlign.Center,
            )
        }
    }

    @Composable
    fun TasksAccount(
        skus: List<Sku>,
        subscribe: (Int, Boolean) -> Unit,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KEYLINE_FIRST, 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                PurchaseButton(
                    price = remember(skus) {
                        skus.find { it.productId == "annual_30" }?.price ?: "$30"
                    },
                    popperText = "${stringResource(R.string.save_percent, 16)} $POPPER",
                    onClick = { subscribe(30, false) },
                )
                Spacer(Modifier.width(KEYLINE_FIRST))
                PurchaseButton(
                    price = remember(skus) {
                        skus.find { it.productId == "monthly_03" }?.price ?: "$3"
                    },
                    monthly = true,
                    onClick = { subscribe(3, true) },
                )
            }
        }
    }

    @Composable
    fun PurchaseButton(
        price: String,
        monthly: Boolean = false,
        popperText: String = "",
        onClick: () -> Unit,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { onClick() },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = stringResource(
                        if (monthly) R.string.price_per_month_with_currency else R.string.price_per_year_with_currency,
                        price
                    ),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = popperText,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    @Composable
    fun NameYourPrice(
        sliderPosition: Float,
        setPrice: (Float) -> Unit,
        subscribe: (Int, Boolean) -> Unit,
        skus: List<Sku>,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) {
                Slider(
                    modifier = Modifier.padding(KEYLINE_FIRST, 0.dp, KEYLINE_FIRST, HALF_KEYLINE),
                    value = sliderPosition,
                    onValueChange = { setPrice(it) },
                    valueRange = 1f..25f,
                    steps = 25,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = colorResource(R.color.text_tertiary),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val price = sliderPosition.toInt()
                PurchaseButton(
                    price = remember(skus, price) {
                        skus
                            .find { it.productId == "annual_${price.toString().padStart(2, '0')}" }
                            ?.price
                            ?: "$$price"
                    },
                    popperText = if (sliderPosition.toInt() >= 7)
                        "${stringResource(R.string.above_average, 16)} $POPPER"
                    else
                        "",
                    onClick = { subscribe(sliderPosition.toInt(), false) },
                )
                if (sliderPosition.toInt() < 3) {
                    Spacer(Modifier.width(KEYLINE_FIRST))
                    PurchaseButton(
                        price = remember(skus, price) {
                            skus
                                .find { it.productId == "monthly_${price.toString().padStart(2, '0')}" }
                                ?.price
                                ?: "$$price"
                        },
                        monthly = true,
                        popperText = "${stringResource(R.string.above_average)} $POPPER",
                        onClick = { subscribe(price, true) },
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@PreviewFontScale
@PreviewScreenSizes
@Composable
private fun PurchaseDialogPreview() {
    TasksTheme {
        SubscriptionScreen(
            subscribe = { _, _ -> },
            onBack = {},
            nameYourPrice = false,
            sliderPosition = 1f,
            setPrice = {},
            setNameYourPrice = {},
            skus = emptyList(),
        )
    }
}

@PreviewLightDark
@Composable
private fun NameYourPricePreview() {
    TasksTheme {
        SubscriptionScreen(
            subscribe = { _, _ -> },
            onBack = {},
            nameYourPrice = true,
            sliderPosition = 4f,
            setPrice = {},
            setNameYourPrice = {},
            skus = emptyList(),
        )
    }
}

@PreviewLightDark
@Composable
private fun UpgradeNoSubscriptionPreview() {
    TasksTheme {
        SubscriptionScreen(
            subscribe = { _, _ -> },
            onBack = {},
            nameYourPrice = false,
            showMoreOptions = false,
            sliderPosition = 1f,
            setPrice = {},
            setNameYourPrice = {},
            skus = emptyList(),
        )
    }
}

@PreviewLightDark
@Composable
private fun UpgradeExistingSubscriberPreview() {
    TasksTheme {
        SubscriptionScreen(
            subscribe = { _, _ -> },
            onBack = {},
            nameYourPrice = false,
            showMoreOptions = false,
            existingSubscriber = true,
            sliderPosition = 1f,
            setPrice = {},
            setNameYourPrice = {},
            skus = emptyList(),
        )
    }
}
