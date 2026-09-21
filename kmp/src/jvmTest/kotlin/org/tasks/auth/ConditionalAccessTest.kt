package org.tasks.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionalAccessTest {

    @Test
    fun detectsCompliantDeviceBlockByCode() {
        assertTrue(
            ConditionalAccess.isDevicePolicyError(
                "access_denied",
                "AADSTS53000: Device is not in required device state: compliant. Conditional Access " +
                    "policy requires a compliant device, and the device is not compliant.",
            )
        )
    }

    @Test
    fun detectsBlockByCodeEvenWhenDescriptionIsLocalized() {
        assertTrue(
            ConditionalAccess.isDevicePolicyError(
                "access_denied",
                "AADSTS53000: Das Gerät befindet sich nicht im erforderlichen Gerätestatus.",
            )
        )
    }

    @Test
    fun ignoresEnglishDevicePhraseWithoutCode() {
        assertFalse(
            ConditionalAccess.isDevicePolicyError(
                "access_denied",
                "Your organization requires a managed device to access this resource.",
            )
        )
    }

    @Test
    fun ignoresUnrelatedErrors() {
        assertFalse(
            ConditionalAccess.isDevicePolicyError(
                "invalid_grant",
                "AADSTS70008: The provided authorization code or refresh token has expired.",
            )
        )
        assertFalse(ConditionalAccess.isDevicePolicyError(null, null))
        assertFalse(ConditionalAccess.isDevicePolicyError("", ""))
    }

    @Test
    fun devicePolicyExceptionCarriesFriendlyMessage() {
        val exception = ConditionalAccess.devicePolicyException(
            "access_denied",
            "AADSTS53000: Device is not compliant. Access has been blocked by Conditional Access " +
                "policies.",
        )
        assertEquals(ConditionalAccess.MESSAGE, exception?.message)
    }

    @Test
    fun devicePolicyExceptionNullForUnrelatedError() {
        assertNull(
            ConditionalAccess.devicePolicyException(
                "invalid_client",
                "AADSTS7000215: Invalid client secret provided.",
            )
        )
    }
}
