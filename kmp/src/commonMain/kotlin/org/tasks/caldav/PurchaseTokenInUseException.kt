package org.tasks.caldav

import kotlinx.io.IOException

class PurchaseTokenInUseException(
    val existingAccount: String,
) : IOException("purchase_token_in_use: $existingAccount")
