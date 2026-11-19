package org.tasks.http

import kotlinx.serialization.builtins.serializer
import org.tasks.security.KeyStoreEncryption
import java.io.File

object DesktopCookieStorage {
    fun forKey(cookieDir: File, key: String?, encryption: KeyStoreEncryption): PersistentCookiesStorage {
        val file = EncryptedFile(cookieFile(cookieDir, key, "msgraph"), encryption)
        return PersistentCookiesStorage(
            object : CookieBackend {
                override suspend fun read() = file.readList(String.serializer())

                override suspend fun write(cookies: List<String>) =
                    file.writeList(String.serializer(), cookies)

                override suspend fun delete() {
                    file.delete()
                }
            }
        )
    }
}
