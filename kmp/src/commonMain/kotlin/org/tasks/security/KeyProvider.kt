package org.tasks.security

import javax.crypto.SecretKey

interface KeyProvider {
    fun getKey(): SecretKey
}