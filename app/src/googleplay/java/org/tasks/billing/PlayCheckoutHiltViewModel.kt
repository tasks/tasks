package org.tasks.billing

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import org.tasks.analytics.Firebase
import javax.inject.Inject

@HiltViewModel
class PlayCheckoutHiltViewModel @Inject constructor(
    billing: PlayBilling,
    purchases: PlayPurchases,
    reporting: Firebase,
    entitlement: PurchaseEntitlement,
) : PlayCheckoutViewModel(
    billing = billing,
    purchases = purchases,
    reporting = reporting,
    coveredBy = entitlement.coveredBy.map { it?.unlockSource },
)
