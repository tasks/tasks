package org.tasks.viewmodel

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PreferenceWriteQueue(
    private val viewModelScope: CoroutineScope,
    private val persistenceScope: CoroutineScope,
    private val tag: String,
    private val reload: suspend () -> Unit,
) {
    private var pendingWrite: Job? = null

    fun write(block: suspend () -> Unit) {
        val previous = pendingWrite
        pendingWrite = persistenceScope.launch {
            previous?.join()
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e, tag = tag) { "Failed to save settings" }
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            while (true) {
                val write = pendingWrite ?: break
                write.join()
                if (pendingWrite === write) {
                    break
                }
            }
            reload()
        }
    }
}
