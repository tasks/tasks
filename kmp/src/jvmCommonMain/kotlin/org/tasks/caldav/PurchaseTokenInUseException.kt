package org.tasks.caldav

import java.io.IOException

class PurchaseTokenInUseException(
    val existingAccount: String,
) : IOException("purchase_token_in_use: $existingAccount")
