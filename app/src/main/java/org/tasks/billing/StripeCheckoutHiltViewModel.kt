package org.tasks.billing

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.analytics.Firebase
import org.tasks.viewmodel.StripeCheckoutViewModel
import javax.inject.Inject

@HiltViewModel
class StripeCheckoutHiltViewModel @Inject constructor(
    purchases: StripeCheckouts,
    reporting: Firebase,
) : StripeCheckoutViewModel(purchases = purchases, reporting = reporting)
