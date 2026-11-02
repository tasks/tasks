package org.tasks.compose.pricing

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.price_per_month_with_currency
import tasks.kmp.generated.resources.price_per_year_with_currency

private fun intervalResource(isMonthly: Boolean): StringResource = if (isMonthly) {
    Res.string.price_per_month_with_currency
} else {
    Res.string.price_per_year_with_currency
}

@Composable
internal fun intervalPrice(formattedPrice: String, isMonthly: Boolean): String =
    stringResource(intervalResource(isMonthly), formattedPrice)
