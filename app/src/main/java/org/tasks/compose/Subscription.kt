package org.tasks.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.compose.Constants.HALF_KEYLINE
import org.tasks.compose.Constants.KEYLINE_FIRST
import org.tasks.compose.PurchaseText.PurchaseText
import org.tasks.extensions.Context.openUri

object PurchaseText {
    private const val POPPER = "\uD83C\uDF89"

    data class CarouselItem(
        val title: Int,
        val icon: Int,
        val description: Int,
        val tint: Boolean = true
    )

    private val featureList = listOf(
        CarouselItem(
            R.string.tasks_org_account,
            R.drawable.ic_round_icon,
            R.string.upgrade_tasks_org_account_description,
            tint = false
        ),
        CarouselItem(
            R.string.upgrade_more_customization,
            R.drawable.ic_outline_palette_24px,
            R.string.upgrade_more_customization_description
        ),
        CarouselItem(
            R.string.open_source,
            R.drawable.ic_octocat,
            R.string.upgrade_open_source_description
        ),
        CarouselItem(
            R.string.upgrade_desktop_access,
            R.drawable.ic_outline_computer_24px,
            R.string.upgrade_desktop_access_description
        ),
        CarouselItem(
            R.string.gtasks_GPr_header,
            R.drawable.ic_google,
            R.string.upgrade_google_tasks,
            false
        ),
        CarouselItem(
            R.string.davx5,
            R.drawable.ic_davx5_icon_green_bg,
            R.string.davx5_selection_description,
            false
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
            false
        ),
        CarouselItem(
            R.string.decsync,
            R.drawable.ic_decsync,
            R.string.decsync_selection_description,
            false
        ),
        CarouselItem(
            R.string.upgrade_automation,
            R.drawable.ic_tasker,
            R.string.upgrade_automation_description,
            false,
        )
    )

    @Composable
    fun PurchaseText(
        nameYourPrice: MutableState<Boolean> = mutableStateOf(false),
        sliderPosition: MutableState<Float> = mutableStateOf(0f),
        github: Boolean = false,
        solidButton: Boolean = false,
        badge: Boolean = false,
        onDisplayed: () -> Unit = {},
        subscribe: (Int, Boolean) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(color = colorResource(R.color.content_background)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GreetingText(R.string.upgrade_blurb_1)
            GreetingText(R.string.upgrade_blurb_2)
            Spacer(Modifier.height(KEYLINE_FIRST))
            val pagerState = remember {
                PagerState(maxPage = (featureList.size - 1).coerceAtLeast(0))
            }
            Pager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                PagerItem(featureList[page], nameYourPrice.value && page == 0)
            }
            if (github) {
                SponsorButton()
            } else {
                GooglePlayButtons(
                    nameYourPrice = nameYourPrice,
                    sliderPosition = sliderPosition,
                    pagerState = pagerState,
                    subscribe = subscribe,
                    solidButton = solidButton,
                    badge = badge,
                )
            }
        }
        LaunchedEffect(key1 = Unit) {
            onDisplayed()
        }
    }

    @Composable
    fun SponsorButton() {
        val context = LocalContext.current
        OutlinedButton(
            onClick = { context.openUri(R.string.url_sponsor) },
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.onSecondary
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
                    color = MaterialTheme.colors.onSecondary,
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }

    @Composable
    fun GreetingText(resId: Int) {
        Text(
            modifier = Modifier.padding(KEYLINE_FIRST, KEYLINE_FIRST, KEYLINE_FIRST, 0.dp),
            text = stringResource(resId),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
        )
    }

    @Composable
    fun GooglePlayButtons(
        nameYourPrice: MutableState<Boolean>,
        sliderPosition: MutableState<Float>,
        pagerState: PagerState,
        subscribe: (Int, Boolean) -> Unit,
        solidButton: Boolean,
        badge: Boolean,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Divider(color = MaterialTheme.colors.onSurface, thickness = 0.25.dp)
            Spacer(Modifier.height(KEYLINE_FIRST))
            if (nameYourPrice.value) {
                NameYourPrice(sliderPosition, subscribe)
            } else {
                TasksAccount(subscribe)
            }
            Spacer(Modifier.height(KEYLINE_FIRST))
            OutlinedButton(
                onClick = {
                    nameYourPrice.value = !nameYourPrice.value
                    pagerState.currentPage = 0
                },
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = if (solidButton)
                        MaterialTheme.colors.secondary
                    else
                        Color.Transparent
                )
            ) {
                BadgedBox(badge = {
                    if (!nameYourPrice.value && badge) {
                        Badge()
                    }
                }) {
                    Text(
                        text = stringResource(
                            if (nameYourPrice.value)
                                R.string.back
                            else
                                R.string.more_options
                        ),
                        color = if (solidButton)
                            MaterialTheme.colors.onSecondary
                        else
                            MaterialTheme.colors.secondary,
                        style = MaterialTheme.typography.body1
                    )
                }
            }
            Text(
                text = stringResource(R.string.pro_free_trial),
                style = MaterialTheme.typography.caption,
                modifier = Modifier
                    .fillMaxWidth(.75f)
                    .padding(KEYLINE_FIRST),
                color = MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center,
            )
        }
    }

    @Composable
    fun PagerItem(
        feature: CarouselItem,
        disabled: Boolean = false
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(.5f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HALF_KEYLINE),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(feature.icon),
                        contentDescription = null,
                        modifier = Modifier.requiredSize(72.dp),
                        alignment = Alignment.Center,
                        colorFilter = if (feature.tint) {
                            ColorFilter.tint(colorResource(R.color.icon_tint_with_alpha))
                        } else {
                            null
                        }
                    )
                    Text(
                        text = stringResource(feature.title),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp, 4.dp),
                        color = MaterialTheme.colors.onBackground,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.25.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(if (disabled) R.string.account_not_included else feature.description),
                        modifier = Modifier.fillMaxWidth(),
                        color = if (disabled) Color.Red else MaterialTheme.colors.onBackground,
                        style = TextStyle(
                            fontWeight = if (disabled) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            letterSpacing = 0.4.sp
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    @Composable
    fun TasksAccount(subscribe: (Int, Boolean) -> Unit) {
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
                    price = 30,
                    popperText = "${stringResource(R.string.save_percent, 16)} $POPPER",
                    onClick = subscribe
                )
                Spacer(Modifier.width(KEYLINE_FIRST))
                PurchaseButton(
                    price = 3,
                    monthly = true,
                    onClick = subscribe
                )
            }
        }
    }

    @Composable
    fun PurchaseButton(
        price: Int,
        monthly: Boolean = false,
        popperText: String = "",
        onClick: (Int, Boolean) -> Unit
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { onClick(price, monthly) },
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = MaterialTheme.colors.secondary
                )
            ) {
                Text(
                    text = stringResource(
                        if (monthly) R.string.price_per_month else R.string.price_per_year,
                        price
                    ),
                    color = MaterialTheme.colors.onSecondary,
                    style = MaterialTheme.typography.body1
                )
            }
            Text(
                text = popperText,
                color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.caption,
            )
        }
    }

    @Composable
    fun NameYourPrice(sliderPosition: MutableState<Float>, subscribe: (Int, Boolean) -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) {
                Slider(
                    modifier = Modifier.padding(KEYLINE_FIRST, 0.dp, KEYLINE_FIRST, HALF_KEYLINE),
                    value = sliderPosition.value,
                    onValueChange = { sliderPosition.value = it },
                    valueRange = 1f..25f,
                    steps = 25,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colors.secondary,
                        activeTrackColor = MaterialTheme.colors.secondary,
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
                PurchaseButton(
                    price = sliderPosition.value.toInt(),
                    popperText = if (sliderPosition.value.toInt() >= 5)
                        "${stringResource(R.string.above_average, 16)} $POPPER"
                    else
                        "",
                    onClick = subscribe
                )
                if (sliderPosition.value.toInt() < 3) {
                    Spacer(Modifier.width(KEYLINE_FIRST))
                    PurchaseButton(
                        price = sliderPosition.value.toInt(),
                        monthly = true,
                        popperText = "${stringResource(R.string.above_average)} $POPPER",
                        onClick = subscribe
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PurchaseDialogPreview() {
    MdcTheme {
        PurchaseText { _, _ -> }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PurchaseDialogPreviewSolid() {
    MdcTheme {
        PurchaseText(solidButton = true) { _, _ -> }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PurchaseDialogPreviewBadge() {
    MdcTheme {
        PurchaseText(badge = true) { _, _ -> }
    }
}
