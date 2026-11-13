package org.tasks.billing

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import org.tasks.analytics.Firebase
import org.tasks.injection.ApplicationScope
import org.tasks.viewmodel.StripeCheckoutViewModel
import javax.inject.Inject

@HiltViewModel
class StripeCheckoutHiltViewModel @Inject constructor(
    purchases: StripeCheckouts,
    reporting: Firebase,
    @ApplicationScope scope: CoroutineScope,
) : StripeCheckoutViewModel(purchases = purchases, reporting = reporting, background = scope)
