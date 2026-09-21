package org.tasks.auth

object ConditionalAccess {
    private val DEVICE_POLICY_CODES = listOf(
        "AADSTS53000", // Device is not compliant
        "AADSTS53001", // Device is not domain / hybrid-joined
        "AADSTS53002", // App protection policy required
        "AADSTS53004", // Device must be enrolled / registered (MDM)
        "AADSTS50097", // Device authentication required
    )


    const val MESSAGE =
        "This work or school account is protected by a Conditional Access policy that requires a " +
        "managed or compliant device, which desktop sign-in can't satisfy. Sign in from the Tasks " +
        "mobile app or contact your IT administrator."

    fun isDevicePolicyError(vararg fields: String?): Boolean {
        val haystack = fields.filterNotNull().joinToString(" ").lowercase()
        if (haystack.isBlank()) return false
        return DEVICE_POLICY_CODES.any { haystack.contains(it.lowercase()) }
    }

    fun devicePolicyException(error: String?, description: String?): Exception? =
        if (isDevicePolicyError(error, description)) Exception(MESSAGE) else null
}
