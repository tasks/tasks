package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.tasks.billing.PurchaseTerms
import org.tasks.billing.Renewal
import org.tasks.billing.renewal
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.subscription_ended_on
import tasks.kmp.generated.resources.subscription_ends_on
import tasks.kmp.generated.resources.subscription_free_until
import tasks.kmp.generated.resources.subscription_payment_failed
import tasks.kmp.generated.resources.subscription_renews_on

@Composable
internal fun PurchaseTerms.renewalNote(price: String?): String? =
    when (val renewal = renewal(price)) {
        null -> null
        is Renewal.Trial -> formattedDate(renewal.until)?.let {
            stringResource(Res.string.subscription_free_until, it, renewal.price)
        }
        Renewal.PaymentFailed -> stringResource(Res.string.subscription_payment_failed)
        is Renewal.Ends -> formattedDate(renewal.on)?.let {
            stringResource(Res.string.subscription_ends_on, it)
        }
        is Renewal.Ended -> formattedDate(renewal.on)?.let {
            stringResource(Res.string.subscription_ended_on, it)
        }
        is Renewal.Renews -> formattedDate(renewal.on)?.let {
            stringResource(Res.string.subscription_renews_on, it)
        }
    }
