package org.tasks.security

import co.touchlab.kermit.Logger
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.posix.memcpy
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFErrorGetCode
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemCopyMatching
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateDecryptedData
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyRef
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAccessControlPrivateKeyUsage
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrIsPermanent
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecAttrTokenID
import platform.Security.kSecAttrTokenIDSecureEnclave
import platform.Security.kSecClass
import platform.Security.kSecClassKey
import platform.Security.kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM
import platform.Security.kSecPrivateKeyAttrs
import platform.Security.kSecReturnRef

private const val TAG = "SecureEnclaveEncryption"

private const val KEY_TAG = "org.tasks.passwords"

@OptIn(ExperimentalForeignApi::class)
class SecureEnclaveEncryption : Encryption {
    private val logger = Logger.withTag(TAG)

    private val key: SecKeyRef? by lazy {
        loadKey() ?: createKey(secureEnclave = true) ?: createKey(secureEnclave = false)
    }

    override suspend fun encrypt(text: String): String? = withContext(Dispatchers.Default) {
        val publicKey = key?.let { SecKeyCopyPublicKey(it) } ?: run {
            logger.e { "No encryption key" }
            return@withContext null
        }
        try {
            transform(text.encodeToByteArray()) { data, error ->
                SecKeyCreateEncryptedData(publicKey, ALGORITHM, data, error)
            }
                ?.toByteString()
                ?.base64()
        } finally {
            CFRelease(publicKey)
        }
    }

    override suspend fun decrypt(text: String?): String? = withContext(Dispatchers.Default) {
        if (text.isNullOrBlank()) {
            return@withContext null
        }
        val privateKey = key ?: return@withContext ""
        val ciphertext = text.decodeBase64()?.toByteArray() ?: run {
            logger.e { "Failed to decode Base64 data" }
            return@withContext ""
        }
        transform(ciphertext) { data, error ->
            SecKeyCreateDecryptedData(privateKey, ALGORITHM, data, error)
        }
            ?.decodeToString()
            ?: ""
    }

    private inline fun transform(
        input: ByteArray,
        block: (CFDataRef?, CPointer<CFErrorRefVar>) -> CFDataRef?,
    ): ByteArray? = memScoped {
        if (input.isEmpty()) {
            return null
        }
        val data = input.toCFData() ?: return null
        val error = alloc<CFErrorRefVar>()
        val output = block(data, error.ptr)
        CFRelease(data)
        if (output == null) {
            logger.e { "Security framework error ${error.value?.let { CFErrorGetCode(it) }}" }
            error.value?.let { CFRelease(it) }
            return null
        }
        output.toByteArray().also { CFRelease(output) }
    }

    private fun loadKey(): SecKeyRef? = memScoped {
        val tag = KEY_TAG.encodeToByteArray().toCFData() ?: return null
        val query = newDictionary()
        CFDictionarySetValue(query, kSecClass, kSecClassKey)
        CFDictionarySetValue(query, kSecAttrApplicationTag, tag)
        CFDictionarySetValue(query, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
        CFDictionarySetValue(query, kSecReturnRef, kCFBooleanTrue)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(tag)
        CFRelease(query)
        when (status) {
            errSecSuccess -> result.value?.reinterpret()
            errSecItemNotFound -> null
            else -> {
                logger.w { "Keychain lookup failed: $status" }
                null
            }
        }
    }

    private fun createKey(secureEnclave: Boolean): SecKeyRef? = memScoped {
        val error = alloc<CFErrorRefVar>()
        val tag = KEY_TAG.encodeToByteArray().toCFData() ?: return null
        val privateAttributes = newDictionary()
        CFDictionarySetValue(privateAttributes, kSecAttrIsPermanent, kCFBooleanTrue)
        CFDictionarySetValue(privateAttributes, kSecAttrApplicationTag, tag)
        val accessControl = if (secureEnclave) {
            SecAccessControlCreateWithFlags(
                kCFAllocatorDefault,
                kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                kSecAccessControlPrivateKeyUsage,
                error.ptr,
            )
        } else {
            null
        }
        if (secureEnclave && accessControl == null) {
            logger.w { "No access control: ${error.value?.let { CFErrorGetCode(it) }}" }
            error.value?.let { CFRelease(it) }
            CFRelease(privateAttributes)
            CFRelease(tag)
            return null
        }
        if (accessControl != null) {
            CFDictionarySetValue(privateAttributes, kSecAttrAccessControl, accessControl)
        } else {
            CFDictionarySetValue(
                privateAttributes,
                kSecAttrAccessible,
                kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            )
        }
        val attributes = newDictionary()
        CFDictionarySetValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
        val keySize = alloc<platform.posix.int32_tVar>().apply { value = 256 }
        val size = CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, keySize.ptr)
        CFDictionarySetValue(attributes, kSecAttrKeySizeInBits, size)
        if (secureEnclave) {
            CFDictionarySetValue(attributes, kSecAttrTokenID, kSecAttrTokenIDSecureEnclave)
        }
        CFDictionarySetValue(attributes, kSecPrivateKeyAttrs, privateAttributes)
        val created = SecKeyCreateRandomKey(attributes, error.ptr)
        CFRelease(attributes)
        CFRelease(privateAttributes)
        accessControl?.let { CFRelease(it) }
        CFRelease(size)
        CFRelease(tag)
        if (created == null) {
            val code = error.value?.let { CFErrorGetCode(it) }
            error.value?.let { CFRelease(it) }
            if (secureEnclave) {
                logger.w { "Secure Enclave unavailable ($code), falling back to a software key" }
            } else {
                logger.e { "Failed to create key: $code" }
            }
            null
        } else {
            logger.i { "Generated ${if (secureEnclave) "Secure Enclave" else "software"} key" }
            created
        }
    }

    private fun newDictionary(): CFMutableDictionaryRef = CFDictionaryCreateMutable(
        kCFAllocatorDefault,
        0,
        kCFTypeDictionaryKeyCallBacks.ptr,
        kCFTypeDictionaryValueCallBacks.ptr,
    )!!

    private fun ByteArray.toCFData(): CFDataRef? = usePinned {
        CFDataCreate(kCFAllocatorDefault, it.addressOf(0).reinterpret(), size.convert())
    }

    private fun CFDataRef.toByteArray(): ByteArray {
        val length = CFDataGetLength(this).toInt()
        val bytes = CFDataGetBytePtr(this)
        if (length == 0 || bytes == null) {
            return ByteArray(0)
        }
        return ByteArray(length).apply {
            usePinned { memcpy(it.addressOf(0), bytes, length.convert()) }
        }
    }

    companion object {
        private val ALGORITHM = kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM
    }
}
