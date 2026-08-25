package org.tasks.ai

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class InMemoryDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

internal fun testEncryption() = org.tasks.security.KeyStoreEncryption(
    object : org.tasks.security.KeyProvider {
        private val key: javax.crypto.SecretKey =
            javax.crypto.KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        override fun getKey() = key
    }
)
