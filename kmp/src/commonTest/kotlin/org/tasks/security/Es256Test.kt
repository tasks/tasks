package org.tasks.security

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Es256Test {
    @Test
    fun verifiesASignatureFromTheMatchingKey() {
        assertTrue(verify(SIGNING_INPUT, SIGNATURE))
    }

    @Test
    fun rejectsASignatureOverDifferentBytes() {
        assertFalse(verify(SIGNING_INPUT.dropLast(1), SIGNATURE))
    }

    @Test
    fun rejectsATamperedSignature() {
        val tampered = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
            .decode(SIGNATURE)
            .also { it[0] = (it[0] + 1).toByte() }
        assertFalse(
            verifyEs256(
                publicKey = Base64.Default.decode(PUBLIC_KEY),
                signed = SIGNING_INPUT.encodeToByteArray(),
                derSignature = assertNotNull(rawEcdsaToDer(tampered)),
            )
        )
    }

    @Test
    fun rejectsASignatureFromAnotherKey() {
        assertFalse(
            verifyEs256(
                publicKey = Base64.Default.decode(OTHER_PUBLIC_KEY),
                signed = SIGNING_INPUT.encodeToByteArray(),
                derSignature = assertNotNull(rawEcdsaToDer(rawSignature())),
            )
        )
    }

    @Test
    fun rejectsAPublicKeyThatIsNotAKey() {
        assertFalse(
            verifyEs256(
                publicKey = byteArrayOf(1, 2, 3),
                signed = SIGNING_INPUT.encodeToByteArray(),
                derSignature = assertNotNull(rawEcdsaToDer(rawSignature())),
            )
        )
    }

    @Test
    fun derEncodingRejectsAnythingButSixtyFourBytes() {
        assertNull(rawEcdsaToDer(ByteArray(63)))
        assertNull(rawEcdsaToDer(ByteArray(65)))
    }

    @Test
    fun derEncodingPadsComponentsWithAHighBit() {
        val der = assertNotNull(rawEcdsaToDer(ByteArray(64) { -1 }))
        assertEquals("3046022100" + "ff".repeat(32) + "022100" + "ff".repeat(32), der.toHexString())
    }

    @Test
    fun derEncodingTrimsLeadingZeros() {
        val raw = ByteArray(64).also { it[31] = 1; it[63] = 2 }
        val der = assertNotNull(rawEcdsaToDer(raw))
        assertEquals("3006020101020102", der.toHexString())
    }

    private fun rawSignature(): ByteArray =
        Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(SIGNATURE)

    private fun verify(signed: String, signature: String): Boolean {
        val raw = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(signature)
        return verifyEs256(
            publicKey = Base64.Default.decode(PUBLIC_KEY),
            signed = signed.encodeToByteArray(),
            derSignature = assertNotNull(rawEcdsaToDer(raw)),
        )
    }

    companion object {
        private const val PUBLIC_KEY =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEJJCcqrIYMPdKFVsZqNMHXhvkTezr" +
            "Xn01/i7kSC/E/LuT1s98GPflX1jEHbJ27YX8jKsQ3yLiuHixqbY9QDikFQ=="
        private const val OTHER_PUBLIC_KEY =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1ZGBhWUqfRRg78YGyVchzC0y9Ugh" +
            "SXVw/oVv5itVIzZHovcXs8di7X7zeDfNYlHv+nHaGExFI7y6QxjJ/+NasQ=="
        private const val SIGNING_INPUT = "eyJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJ0YXNrcyJ9"
        private const val SIGNATURE =
            "FIIjLzHR_ooDyzVTwL8eBuyMmxK7H2ubv1IbtPCLAHDDh40gzXKHNbCXUcSNCf2W" +
            "e9bvA8lV4Ub8Rr1Y7veEhg"
    }
}
