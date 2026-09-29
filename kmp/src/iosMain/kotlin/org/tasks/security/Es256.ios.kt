package org.tasks.security

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFErrorGetCode
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecKeyCreateWithData
import platform.Security.SecKeyVerifySignature
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256

private val logger = Logger.withTag("Es256")

@OptIn(ExperimentalForeignApi::class)
internal actual fun verifyEs256(
    publicKey: ByteArray,
    signed: ByteArray,
    derSignature: ByteArray,
): Boolean = memScoped {
    val point = publicKey.ecPointFromSpki() ?: run {
        logger.e { "Public key is not a P-256 SubjectPublicKeyInfo" }
        return false
    }
    val keyData = point.toCFData() ?: return false
    val attributes = newDictionary()
    CFDictionarySetValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
    CFDictionarySetValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)
    val error = alloc<CFErrorRefVar>()
    val key = SecKeyCreateWithData(keyData, attributes, error.ptr)
    CFRelease(keyData)
    CFRelease(attributes)
    if (key == null) {
        logger.e { "Failed to load public key: ${error.value?.let { CFErrorGetCode(it) }}" }
        error.value?.let { CFRelease(it) }
        return false
    }
    try {
        val signedData = signed.toCFData() ?: return false
        val signatureData = derSignature.toCFData() ?: run {
            CFRelease(signedData)
            return false
        }
        val verified = SecKeyVerifySignature(
            key,
            kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
            signedData,
            signatureData,
            error.ptr,
        )
        CFRelease(signedData)
        CFRelease(signatureData)
        if (!verified) {
            error.value?.let { CFRelease(it) }
        }
        verified
    } finally {
        CFRelease(key)
    }
}

private const val UNCOMPRESSED_POINT: Byte = 0x04
private const val UNCOMPRESSED_P256_POINT_SIZE = 65

private fun ByteArray.ecPointFromSpki(): ByteArray? {
    val reader = DerReader(this)
    val spki = reader.readSequence() ?: return null
    if (spki.readSequence() == null) return null
    val bitString = spki.read(DER_BIT_STRING) ?: return null
    val unusedBits = bitString.firstOrNull() ?: return null
    if (unusedBits != 0.toByte()) return null
    val point = bitString.copyOfRange(1, bitString.size)
    return point.takeIf {
        it.size == UNCOMPRESSED_P256_POINT_SIZE && it[0] == UNCOMPRESSED_POINT
    }
}

private class DerReader(private val bytes: ByteArray, private var offset: Int = 0) {
    fun readSequence(): DerReader? = read(DER_SEQUENCE)?.let { DerReader(it) }

    fun read(tag: Byte): ByteArray? {
        if (offset >= bytes.size || bytes[offset] != tag) return null
        offset++
        val length = readLength() ?: return null
        if (length < 0 || offset + length > bytes.size) return null
        return bytes.copyOfRange(offset, offset + length).also { offset += length }
    }

    private fun readLength(): Int? {
        if (offset >= bytes.size) return null
        val first = bytes[offset++].toInt() and 0xff
        if (first and 0x80 == 0) return first
        val count = first and 0x7f
        if (count == 0 || count > 3 || offset + count > bytes.size) return null
        var length = 0
        repeat(count) { length = (length shl 8) or (bytes[offset++].toInt() and 0xff) }
        return length
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun newDictionary(): CFMutableDictionaryRef = CFDictionaryCreateMutable(
    kCFAllocatorDefault,
    0,
    kCFTypeDictionaryKeyCallBacks.ptr,
    kCFTypeDictionaryValueCallBacks.ptr,
)!!

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toCFData(): CFDataRef? =
    if (isEmpty()) null else usePinned {
        CFDataCreate(kCFAllocatorDefault, it.addressOf(0).reinterpret(), size.convert())
    }
