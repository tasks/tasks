package org.tasks.security

internal const val DER_SEQUENCE: Byte = 0x30
internal const val DER_INTEGER: Byte = 0x02
internal const val DER_BIT_STRING: Byte = 0x03

internal expect fun verifyEs256(
    publicKey: ByteArray,
    signed: ByteArray,
    derSignature: ByteArray,
): Boolean

internal fun rawEcdsaToDer(raw: ByteArray): ByteArray? {
    if (raw.size != 64) return null
    val r = derInteger(raw.copyOfRange(0, 32))
    val s = derInteger(raw.copyOfRange(32, 64))
    return byteArrayOf(DER_SEQUENCE, (r.size + s.size).toByte()) + r + s
}

private fun derInteger(component: ByteArray): ByteArray {
    val magnitude = component.dropLeadingZeros()
    val needsSignByte = magnitude[0].toInt() and 0x80 != 0
    val content = if (needsSignByte) byteArrayOf(0x00) + magnitude else magnitude
    return byteArrayOf(DER_INTEGER, content.size.toByte()) + content
}

private fun ByteArray.dropLeadingZeros(): ByteArray {
    var start = 0
    while (start < size - 1 && this[start] == 0.toByte()) start++
    return if (start == 0) this else copyOfRange(start, size)
}
